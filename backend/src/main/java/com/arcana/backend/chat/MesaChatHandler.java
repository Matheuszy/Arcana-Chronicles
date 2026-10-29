package com.arcana.backend.chat;

import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.MesaEncerradaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket para o chat da mesa.
 * URL: ws://localhost:8080/ws/mesas/{tableId}
 *
 * Cada mesa tem seu próprio conjunto de sessões ativas.
 * Quando uma mensagem chega, é rebroadcastada para todos os participantes da mesma mesa.
 * Mensagens com @menção ficam marcadas com mentionedNpcId — a integração com IA entrará aqui.
 */
@Component
public class MesaChatHandler extends TextWebSocketHandler {

    public static final int LIMITE_CARACTERES_TEXTO = 2_000;

    private final ObjectMapper objectMapper;
    private final RolagemDadosService rolagemDadosService;
    private final GerenciadorMesasAtivas gerenciadorMesas;
    private final TaskScheduler agendador;
    private final Clock clock;
    private final Duration toleranciaMestre;

    private final Map<String, WebSocketSession> sessoesComEnvioSerializado = new ConcurrentHashMap<>();

    private static final int LIMITE_TEMPO_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER_ENVIO_BYTES = 512 * 1024;
    private static final Set<ChatMessage.Type> TIPOS_ENVIADOS_PELO_CLIENTE =
            EnumSet.of(ChatMessage.Type.TEXTO, ChatMessage.Type.ROLAGEM);

    public MesaChatHandler(ObjectMapper objectMapper,
                           RolagemDadosService rolagemDadosService,
                           GerenciadorMesasAtivas gerenciadorMesas,
                           @Qualifier("chatTaskScheduler") TaskScheduler agendador,
                           Clock clock,
                           @Value("${arcana.chat.tolerancia-mestre:60s}") Duration toleranciaMestre) {
        this.objectMapper = objectMapper;
        this.rolagemDadosService = rolagemDadosService;
        this.gerenciadorMesas = gerenciadorMesas;
        this.agendador = agendador;
        this.clock = clock;
        this.toleranciaMestre = toleranciaMestre;
    }

    // ── Ciclo de vida da sessão ───────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        AcessoMesaDto acesso = acessoDaSessao(session);
        if (acesso == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String tableId = String.valueOf(acesso.mesaId());
        WebSocketSession sessaoSegura = new ConcurrentWebSocketSessionDecorator(
                session, LIMITE_TEMPO_ENVIO_MS, LIMITE_BUFFER_ENVIO_BYTES);
        sessoesComEnvioSerializado.put(session.getId(), sessaoSegura);

        GerenciadorMesasAtivas.Entrada entrada = gerenciadorMesas.entrar(acesso, session.getId(), sessaoSegura);
        MesaAtiva mesa = entrada.mesa();
        boolean mestreVoltou = ehMestre(acesso) && mesa.cancelarEncerramento();

        if (entrada.primeiraConexaoDoUsuario()) {
            String texto = mestreVoltou
                    ? "O mestre " + acesso.displayName() + " voltou. A sessão continua."
                    : acesso.displayName() + " entrou na mesa.";
            ChatMessage aviso = systemMessage(tableId, texto);
            mesa.registrar(aviso);
            broadcast(mesa, aviso, sessaoSegura);
        }

        ChatMessage sincronizacao = eventoDaMesa(tableId, ChatMessage.Type.HISTORICO);
        sincronizacao.setHistorico(mesa.historico());
        sincronizacao.setPresentes(mesa.presentes());
        enviarParaSessao(session, sincronizacao);

        broadcast(mesa, presenca(mesa, tableId), sessaoSegura);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AcessoMesaDto acesso = acessoDaSessao(session);
        sessoesComEnvioSerializado.remove(session.getId());
        if (acesso == null) return;

        gerenciadorMesas.sair(acesso.mesaId(), session.getId())
                .filter(saida -> !saida.mesaEncerrada())
                .ifPresent(saida -> {
                    MesaAtiva mesa = saida.mesa();
                    String tableId = String.valueOf(acesso.mesaId());
                    if (!saida.usuarioAindaConectado()) {
                        boolean mestreSaiu = ehMestre(acesso) && !mesa.mestreConectado();
                        String texto = mestreSaiu
                                ? "O mestre " + acesso.displayName() + " desconectou. A sessão será encerrada em "
                                        + toleranciaMestre.toSeconds() + " segundos se ele não voltar."
                                : acesso.displayName() + " saiu da mesa.";
                        ChatMessage aviso = systemMessage(tableId, texto);
                        mesa.registrar(aviso);
                        broadcast(mesa, aviso, null);
                        if (mestreSaiu) {
                            agendarEncerramentoPorAusenciaDoMestre(mesa);
                        }
                    }
                    broadcast(mesa, presenca(mesa, tableId), null);
                });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void aoEncerrarMesa(MesaEncerradaEvent evento) {
        gerenciadorMesas.encerrar(evento.mesaId())
                .ifPresent(mesa -> finalizarSessao(mesa, "O mestre encerrou a mesa."));
    }

    private void agendarEncerramentoPorAusenciaDoMestre(MesaAtiva mesa) {
        Long mesaId = mesa.getMesaId();
        mesa.agendarEncerramento(agendador.schedule(
                () -> encerrarPorAusenciaDoMestre(mesaId),
                clock.instant().plus(toleranciaMestre)));
    }

    private void encerrarPorAusenciaDoMestre(Long mesaId) {
        gerenciadorMesas.encerrarSe(mesaId, mesa -> !mesa.mestreConectado())
                .ifPresent(mesa -> finalizarSessao(mesa, "O mestre não voltou a tempo. A sessão foi encerrada."));
    }

    private void finalizarSessao(MesaAtiva mesa, String motivo) {
        ChatMessage evento = eventoDaMesa(String.valueOf(mesa.getMesaId()), ChatMessage.Type.SESSAO_ENCERRADA);
        evento.setContent(motivo);
        broadcast(mesa, evento, null);

        for (WebSocketSession sessao : mesa.sessoes()) {
            try {
                sessao.close(CloseStatus.NORMAL.withReason("Sessão encerrada"));
            } catch (IOException | IllegalStateException ignored) {}
        }
    }

    private boolean ehMestre(AcessoMesaDto acesso) {
        return "MESTRE".equals(acesso.role());
    }

    // ── Recepção de mensagens ─────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage raw) throws Exception {
        AcessoMesaDto acesso = acessoDaSessao(session);
        MesaAtiva mesa = acesso == null ? null : gerenciadorMesas.buscar(acesso.mesaId()).orElse(null);
        if (mesa == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        ChatMessage message = objectMapper.readValue(raw.getPayload(), ChatMessage.class);
        String tableId = String.valueOf(acesso.mesaId());

        message.setId(UUID.randomUUID().toString());
        message.setCreatedAt(Instant.now().toString());
        message.setTableId(tableId);
        message.setAuthorId(String.valueOf(acesso.userId()));
        message.setAuthorName(acesso.displayName());
        message.setHistorico(null);
        message.setPresentes(null);

        if (!TIPOS_ENVIADOS_PELO_CLIENTE.contains(message.getType())) {
            enviarParaSessao(session, systemMessage(tableId, "Tipo de mensagem não permitido."));
            return;
        }

        if (message.getType() == ChatMessage.Type.TEXTO) {
            String erro = validarTexto(message.getContent());
            if (erro != null) {
                enviarParaSessao(session, systemMessage(tableId, erro));
                return;
            }
            message.setDiceResult(null);
            message.setRollRequest(null);
        }

        if (message.getType() == ChatMessage.Type.ROLAGEM) {
            try {
                aplicarRolagemDoServidor(message);
            } catch (IllegalArgumentException e) {
                enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
                return;
            }
        }

        // TODO: quando IA estiver integrada, verificar mentionedNpcId aqui
        // e gerar uma ChatMessage do tipo NPC_RESPOSTA assincronamente

        mesa.registrar(message);
        broadcast(mesa, message, null);
    }

    private String validarTexto(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            return "A mensagem está vazia.";
        }
        if (conteudo.length() > LIMITE_CARACTERES_TEXTO) {
            return "A mensagem passa do limite de " + LIMITE_CARACTERES_TEXTO + " caracteres.";
        }
        return null;
    }

    private void aplicarRolagemDoServidor(ChatMessage message) {
        ChatMessage.DiceRollResult resultado = rolagemDadosService.rolar(message.getRollRequest());
        message.setDiceResult(resultado);
        message.setRollRequest(null);
        message.setContent(rolagemDadosService.descrever(resultado));
    }

    private void enviarParaSessao(WebSocketSession session, ChatMessage message) {
        WebSocketSession destino = sessoesComEnvioSerializado.getOrDefault(session.getId(), session);
        try {
            destino.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException | IllegalStateException ignored) {}
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcast(MesaAtiva mesa, ChatMessage message, WebSocketSession exclude) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            return;
        }

        for (WebSocketSession s : mesa.sessoes()) {
            if (s.equals(exclude) || !s.isOpen()) continue;
            try {
                s.sendMessage(new TextMessage(payload));
            } catch (IOException | IllegalStateException ignored) {}
        }
    }

    private AcessoMesaDto acessoDaSessao(WebSocketSession session) {
        Object acesso = session.getAttributes().get(AutenticacaoHandshakeInterceptor.ATRIBUTO_ACESSO);
        return acesso instanceof AcessoMesaDto acessoMesa ? acessoMesa : null;
    }

    private ChatMessage presenca(MesaAtiva mesa, String tableId) {
        ChatMessage evento = eventoDaMesa(tableId, ChatMessage.Type.PRESENCA);
        evento.setPresentes(mesa.presentes());
        return evento;
    }

    private ChatMessage eventoDaMesa(String tableId, ChatMessage.Type type) {
        ChatMessage msg = systemMessage(tableId, null);
        msg.setType(type);
        return msg;
    }

    private ChatMessage systemMessage(String tableId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setTableId(tableId);
        msg.setAuthorId("sistema");
        msg.setAuthorName("Sistema");
        msg.setType(ChatMessage.Type.SISTEMA);
        msg.setContent(content);
        msg.setCreatedAt(Instant.now().toString());
        return msg;
    }
}
