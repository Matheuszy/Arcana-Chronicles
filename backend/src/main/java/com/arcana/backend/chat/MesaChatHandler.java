package com.arcana.backend.chat;

import com.arcana.backend.character.RolagemDeFichaPublicadaEvent;
import com.arcana.backend.chat.dice.ModoRolagem;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.security.LimitadorDeTentativas;
import com.arcana.backend.config.exception.RegraDeNegocioException;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.access.AccessDeniedException;
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

    private static final Logger log = LoggerFactory.getLogger(MesaChatHandler.class);

    public static final int LIMITE_CARACTERES_TEXTO = 2_000;

    private final ObjectMapper objectMapper;
    private final RolagemDadosService rolagemDadosService;
    private final ValidadorImagem validadorImagem;
    private final DesafioChat desafioChat;
    private final SalaChat salaChat;
    private final CombateChat combateChat;
    private final GerenciadorMesasAtivas gerenciadorMesas;
    private final TaskScheduler agendador;
    private final Clock clock;
    private final Duration toleranciaMestre;
    private final LimitadorDeTentativas limitadorDeMensagens;

    private final Map<String, WebSocketSession> sessoesComEnvioSerializado = new ConcurrentHashMap<>();

    public static final int LIMITE_CONEXOES_POR_USUARIO = 5;
    public static final int LIMITE_MENSAGENS_NA_JANELA = 30;
    public static final Duration JANELA_DE_MENSAGENS = Duration.ofSeconds(10);

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
                           DesafioChat desafioChat,
                           SalaChat salaChat,
                           CombateChat combateChat,
                           GerenciadorMesasAtivas gerenciadorMesas,
                           @Qualifier("chatTaskScheduler") TaskScheduler agendador,
                           Clock clock,
                           @Value("${arcana.chat.tolerancia-mestre:60s}") Duration toleranciaMestre) {
        this.objectMapper = objectMapper;
        this.rolagemDadosService = rolagemDadosService;
        this.validadorImagem = validadorImagem;
        this.desafioChat = desafioChat;
        this.salaChat = salaChat;
        this.combateChat = combateChat;
        this.gerenciadorMesas = gerenciadorMesas;
        this.agendador = agendador;
        this.clock = clock;
        this.toleranciaMestre = toleranciaMestre;
        this.limitadorDeMensagens = new LimitadorDeTentativas(LIMITE_MENSAGENS_NA_JANELA, JANELA_DE_MENSAGENS, clock);
    }

    // ── Ciclo de vida da sessão ───────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        AcessoMesaDto acesso = acessoDaSessao(session);
        if (acesso == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        if (conexoesAbertasDoUsuario(acesso) >= LIMITE_CONEXOES_POR_USUARIO) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Conexões demais abertas nesta mesa."));
            return;
        }

        String tableId = String.valueOf(acesso.mesaId());
        WebSocketSession sessaoSegura = new ConcurrentWebSocketSessionDecorator(
                session, LIMITE_TEMPO_ENVIO_MS, LIMITE_BUFFER_ENVIO_BYTES);
        sessoesComEnvioSerializado.put(session.getId(), sessaoSegura);

        GerenciadorMesasAtivas.Entrada entrada = gerenciadorMesas.entrar(acesso, session.getId(), sessaoSegura);
        MesaAtiva mesa = entrada.mesa();
        boolean mestreVoltou = ehMestre(acesso) && mesa.cancelarEncerramento();

        if (entrada.primeiraConexaoDoUsuario() && mestreVoltou) {
            ChatMessage aviso = systemMessage(tableId,
                    "O mestre " + acesso.displayName() + " voltou. A sessão continua.");
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
        desafioChat.sincronizar(mesa, sessaoSegura, acesso.userId());
        salaChat.sincronizar(mesa, sessaoSegura, acesso);
        combateChat.sincronizar(mesa, sessaoSegura, acesso);

        broadcast(mesa, presenca(mesa, tableId), sessaoSegura);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        AcessoMesaDto acesso = acessoDaSessao(session);
        sessoesComEnvioSerializado.remove(session.getId());
        limitadorDeMensagens.esquecer(session.getId());
        if (acesso == null) return;

        gerenciadorMesas.sair(acesso.mesaId(), session.getId())
                .filter(saida -> !saida.mesaEncerrada())
                .ifPresent(saida -> {
                    MesaAtiva mesa = saida.mesa();
                    String tableId = String.valueOf(acesso.mesaId());
                    boolean mestreSaiu = !saida.usuarioAindaConectado() && ehMestre(acesso) && !mesa.mestreConectado();
                    if (mestreSaiu) {
                        ChatMessage aviso = systemMessage(tableId, "O mestre " + acesso.displayName()
                                + " desconectou. A sessão será encerrada em " + toleranciaMestre.toSeconds()
                                + " segundos se o mestre não voltar.");
                        mesa.registrar(aviso);
                        broadcast(mesa, aviso, null);
                        agendarEncerramentoPorAusenciaDoMestre(mesa);
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
            ChatMessage alteracao = eventoDaMesa(tableId, ChatMessage.Type.STATUS_MESA);
            alteracao.setContent(evento.status().name());
            broadcast(mesa, alteracao, null);
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void aoRolarNaFicha(RolagemDeFichaPublicadaEvent evento) {
        gerenciadorMesas.buscar(evento.mesaId()).ifPresent(mesa -> {
            int somaDosDados = evento.dados().stream().mapToInt(Integer::intValue).sum();
            ChatMessage.DiceRollResult resultado = new ChatMessage.DiceRollResult();
            resultado.setExpression(evento.expressao());
            resultado.setSides(evento.lados());
            resultado.setQuantity(evento.dados().size());
            resultado.setMode(ModoRolagem.NORMAL.name());
            resultado.setRolls(evento.dados());
            resultado.setKept(somaDosDados);
            resultado.setModifier(evento.total() - somaDosDados);
            resultado.setTotal(evento.total());
            resultado.setLabel(evento.descricao());

            ChatMessage rolagem = new ChatMessage();
            rolagem.setId(UUID.randomUUID().toString());
            rolagem.setTableId(String.valueOf(evento.mesaId()));
            rolagem.setAuthorId(String.valueOf(evento.autorId()));
            rolagem.setAuthorName(evento.autorNome());
            rolagem.setType(ChatMessage.Type.ROLAGEM);
            rolagem.setCanal(Canal.ROLAGENS);
            rolagem.setDiceResult(resultado);
            rolagem.setContent(rolagemDadosService.descrever(resultado));
            rolagem.setCreatedAt(Instant.now().toString());
            mesa.registrar(rolagem);
            broadcast(mesa, rolagem, null);
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
            desafioChat.aoJogadorSair(mesa, evento.userId());
            salaChat.aoJogadorSair(mesa, evento.userId());
            combateChat.aoJogadorSair(mesa, evento.userId());
            cancelarDesafiosSeCombateAcabou(mesa);
            broadcast(mesa, eventoDaMesa(tableId, ChatMessage.Type.PARTICIPANTES), null);
            broadcast(mesa, presenca(mesa, tableId), null);
        });
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

        String tableId = String.valueOf(acesso.mesaId());
        if (!limitadorDeMensagens.permitir(session.getId())) {
            enviarParaSessao(session, systemMessage(tableId, "Você está enviando mensagens rápido demais. Aguarde um instante."));
            return;
        }

        ChatMessage message;
        try {
            message = objectMapper.readValue(raw.getPayload(), ChatMessage.class);
        } catch (JsonProcessingException e) {
            enviarParaSessao(session, systemMessage(tableId, "Não foi possível entender essa mensagem."));
            return;
        }

        message.setId(UUID.randomUUID().toString());
        message.setCreatedAt(Instant.now().toString());
        message.setTableId(tableId);
        message.setAuthorId(String.valueOf(acesso.userId()));
        message.setAuthorName(acesso.displayName());
        message.setHistorico(null);
        message.setPresentes(null);
        message.setDesafio(null);
        message.setDesafios(null);
        message.setStatus(null);
        message.setItens(null);
        message.setCombates(null);
        message.setEntradaNoCombate(null);

        if (message.getType() == ChatMessage.Type.SESSAO_ENCERRAR) {
            encerrarSessaoPeloMestre(session, acesso);
            return;
        }

        if (message.getType() != null && DesafioChat.COMANDOS.contains(message.getType())) {
            executarComandoDeDesafio(session, acesso, mesa, message);
            return;
        }

        if (message.getType() != null && SalaChat.COMANDOS.contains(message.getType())) {
            executarComandoDaSala(session, acesso, mesa, message);
            return;
        }

        if (message.getType() != null && CombateChat.COMANDOS.contains(message.getType())) {
            executarComandoDeCombate(session, acesso, mesa, message);
            return;
        }

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
            if (desafioChat.jogadorBloqueado(mesa, acesso.userId())) {
                enviarParaSessao(session, systemMessage(tableId,
                        "Você tem um desafio em andamento. A única rolagem permitida é a do desafio."));
                return;
            }
            if (!combateChat.podeRolarLivremente(mesa, acesso.userId())) {
                enviarParaSessao(session, systemMessage(tableId,
                        "Durante o combate você só pode rolar dados no seu turno ou quando for desafiado."));
                return;
            }
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

    private void encerrarSessaoPeloMestre(WebSocketSession session, AcessoMesaDto acesso) {
        if (!ehMestre(acesso)) {
            enviarParaSessao(session, systemMessage(String.valueOf(acesso.mesaId()),
                    "Apenas o mestre pode encerrar a sessão da mesa."));
            return;
        }
        gerenciadorMesas.encerrar(acesso.mesaId()).ifPresent(mesa ->
                finalizarSessao(mesa, "O mestre deixou a sessão. A sessão foi encerrada para todos."));
    }

    private void executarComandoDeDesafio(WebSocketSession session, AcessoMesaDto acesso, MesaAtiva mesa,
                                          ChatMessage message) {
        String tableId = String.valueOf(acesso.mesaId());
        try {
            desafioChat.executar(mesa, acesso, message);
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException
                 | RegraDeNegocioException | RecursoNaoEncontradoException e) {
            enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Falha inesperada no desafio da mesa {}", acesso.mesaId(), e);
            enviarParaSessao(session, systemMessage(tableId, "Não foi possível concluir a ação do desafio. Tente novamente."));
        }
    }

    private void executarComandoDaSala(WebSocketSession session, AcessoMesaDto acesso, MesaAtiva mesa,
                                        ChatMessage message) {
        String tableId = String.valueOf(acesso.mesaId());
        try {
            salaChat.executar(mesa, acesso, message);
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException
                 | RegraDeNegocioException | RecursoNaoEncontradoException e) {
            enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Falha inesperada na sala da mesa {}", acesso.mesaId(), e);
            enviarParaSessao(session, systemMessage(tableId, "Não foi possível concluir a ação. Tente novamente."));
        }
    }

    private void executarComandoDeCombate(WebSocketSession session, AcessoMesaDto acesso, MesaAtiva mesa,
                                          ChatMessage message) {
        String tableId = String.valueOf(acesso.mesaId());
        try {
            combateChat.executar(mesa, acesso, message);
            cancelarDesafiosSeCombateAcabou(mesa);
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException
                 | RegraDeNegocioException | RecursoNaoEncontradoException e) {
            enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Falha inesperada no combate da mesa {}", acesso.mesaId(), e);
            enviarParaSessao(session, systemMessage(tableId, "Não foi possível concluir a ação do combate. Tente novamente."));
        }
    }

    private void cancelarDesafiosSeCombateAcabou(MesaAtiva mesa) {
        desafioChat.aoCombatesEncerrados(mesa, combateChat.combatesAtivos(mesa));
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
        log.warn("Erro de transporte na sessão {}", session.getId(), exception);
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

    private int conexoesAbertasDoUsuario(AcessoMesaDto acesso) {
        return gerenciadorMesas.buscar(acesso.mesaId())
                .map(mesa -> mesa.sessoesDoUsuario(acesso.userId()).size())
                .orElse(0);
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
