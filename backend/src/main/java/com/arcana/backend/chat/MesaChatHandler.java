package com.arcana.backend.chat;

import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.model.Canal;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.ParticipanteRemovidoEvent;
import com.arcana.backend.table.ParticipantesAlteradosEvent;
import com.arcana.backend.table.model.StatusMesa;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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
    private final ValidadorImagem validadorImagem;
    private final GerenciadorMesasAtivas gerenciadorMesas;
    private final TaskScheduler agendador;
    private final Clock clock;
    private final Duration toleranciaMestre;

    private final Map<String, WebSocketSession> sessoesComEnvioSerializado = new ConcurrentHashMap<>();

    private static final int LIMITE_TEMPO_ENVIO_MS = 15_000;
    private static final int LIMITE_BUFFER_ENVIO_BYTES = 16 * 1024 * 1024;
    private static final Set<ChatMessage.Type> TIPOS_ENVIADOS_PELO_CLIENTE =
            EnumSet.of(ChatMessage.Type.TEXTO, ChatMessage.Type.ROLAGEM, ChatMessage.Type.IMAGEM);
    private static final Map<Canal, String> ERRO_CANAL_ERRADO = Map.of(
            Canal.CONVERSA, "Mensagens de texto só podem ser enviadas no canal de conversa.",
            Canal.ROLAGENS, "Rolagens só podem ser enviadas no canal de rolagens.",
            Canal.IMAGENS, "Imagens só podem ser enviadas no canal de imagens.");

    public MesaChatHandler(ObjectMapper objectMapper,
                           RolagemDadosService rolagemDadosService,
                           ValidadorImagem validadorImagem,
                           GerenciadorMesasAtivas gerenciadorMesas,
                           @Qualifier("chatTaskScheduler") TaskScheduler agendador,
                           Clock clock,
                           @Value("${arcana.chat.tolerancia-mestre:60s}") Duration toleranciaMestre) {
        this.objectMapper = objectMapper;
        this.rolagemDadosService = rolagemDadosService;
        this.validadorImagem = validadorImagem;
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

        Map<Canal, List<ChatMessage>> historico = new EnumMap<>(mesa.historicoPorCanal());
        List<ChatMessage> imagens = historico.put(Canal.IMAGENS, List.of());

        ChatMessage sincronizacao = eventoDaMesa(tableId, ChatMessage.Type.HISTORICO);
        sincronizacao.setHistorico(historico);
        sincronizacao.setPresentes(mesa.presentes());
        enviarParaSessao(session, sincronizacao);
        imagens.forEach(imagem -> enviarParaSessao(session, imagem));

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
                                        + toleranciaMestre.toSeconds() + " segundos se o mestre não voltar."
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
    public void aoAlterarStatusDaMesa(MesaStatusAlteradoEvent evento) {
        if (evento.status() == StatusMesa.ENCERRADA) {
            gerenciadorMesas.encerrar(evento.mesaId())
                    .ifPresent(mesa -> finalizarSessao(mesa, "O mestre encerrou a mesa."));
            return;
        }

        gerenciadorMesas.buscar(evento.mesaId()).ifPresent(mesa -> {
            StatusMesa anterior = mesa.alterarStatus(evento.status());
            if (anterior == evento.status()) return;

            String tableId = String.valueOf(evento.mesaId());
            ChatMessage aviso = systemMessage(tableId, avisoDeStatus(anterior, evento.status()));
            mesa.registrar(aviso);
            broadcast(mesa, aviso, null);

            ChatMessage alteracao = eventoDaMesa(tableId, ChatMessage.Type.STATUS_MESA);
            alteracao.setContent(evento.status().name());
            broadcast(mesa, alteracao, null);
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void aoAlterarParticipantes(ParticipantesAlteradosEvent evento) {
        gerenciadorMesas.buscar(evento.mesaId()).ifPresent(mesa ->
                broadcast(mesa, eventoDaMesa(String.valueOf(evento.mesaId()), ChatMessage.Type.PARTICIPANTES), null));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void aoRemoverParticipante(ParticipanteRemovidoEvent evento) {
        String tableId = String.valueOf(evento.mesaId());

        ChatMessage remocao = eventoDaMesa(tableId, ChatMessage.Type.REMOVIDO_DA_MESA);
        remocao.setContent(evento.removidoPeloMestre() ? "O mestre removeu você da mesa." : "Você saiu da mesa.");
        for (WebSocketSession sessao : gerenciadorMesas.desconectarUsuario(evento.mesaId(), evento.userId())) {
            enviarParaSessao(sessao, remocao);
            try {
                sessao.close(CloseStatus.NORMAL.withReason("Removido da mesa"));
            } catch (IOException | IllegalStateException ignored) {}
        }

        gerenciadorMesas.buscar(evento.mesaId()).ifPresent(mesa -> {
            ChatMessage aviso = systemMessage(tableId, evento.removidoPeloMestre()
                    ? "O mestre removeu " + evento.displayName() + " da mesa."
                    : evento.displayName() + " deixou a mesa.");
            mesa.registrar(aviso);
            broadcast(mesa, aviso, null);
            broadcast(mesa, eventoDaMesa(tableId, ChatMessage.Type.PARTICIPANTES), null);
            broadcast(mesa, presenca(mesa, tableId), null);
        });
    }

    private String avisoDeStatus(StatusMesa anterior, StatusMesa novo) {
        if (novo == StatusMesa.PAUSADA) {
            return "O mestre pausou a mesa. Mensagens e rolagens estão bloqueadas.";
        }
        if (anterior == StatusMesa.PAUSADA) {
            return "O mestre retomou a mesa.";
        }
        return novo == StatusMesa.ABERTA
                ? "A mesa está aberta para novos jogadores."
                : "A mesa está em andamento e não aceita novos jogadores.";
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

        Canal canalDoTipo = Canal.doTipo(message.getType());
        if (message.getCanal() != null && message.getCanal() != canalDoTipo) {
            enviarParaSessao(session, systemMessage(tableId, ERRO_CANAL_ERRADO.get(canalDoTipo)));
            return;
        }
        message.setCanal(canalDoTipo);

        if (mesa.pausada()) {
            enviarParaSessao(session, systemMessage(tableId,
                    "A mesa está pausada. Mensagens e rolagens estão bloqueadas até o mestre retomar."));
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
            message.setImage(null);
        }

        if (message.getType() == ChatMessage.Type.ROLAGEM) {
            try {
                aplicarRolagemDoServidor(message);
            } catch (IllegalArgumentException e) {
                enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
                return;
            }
            message.setImage(null);
        }

        if (message.getType() == ChatMessage.Type.IMAGEM) {
            try {
                message.setImage(validadorImagem.validar(message.getImage()));
            } catch (IllegalArgumentException e) {
                enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
                return;
            }
            message.setContent("compartilhou uma imagem.");
            message.setDiceResult(null);
            message.setRollRequest(null);
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
        msg.setCanal(null);
        return msg;
    }

    private ChatMessage systemMessage(String tableId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setTableId(tableId);
        msg.setAuthorId("sistema");
        msg.setAuthorName("Sistema");
        msg.setType(ChatMessage.Type.SISTEMA);
        msg.setCanal(Canal.CONVERSA);
        msg.setContent(content);
        msg.setCreatedAt(Instant.now().toString());
        return msg;
    }
}
