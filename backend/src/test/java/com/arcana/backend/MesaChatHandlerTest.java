package com.arcana.backend;

import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.chat.CombateChat;
import com.arcana.backend.chat.DesafioChat;
import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.SalaChat;
import com.arcana.backend.chat.sala.SalaService;
import com.arcana.backend.chat.combate.CombateService;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.chat.model.Canal;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.ParticipanteRemovidoEvent;
import com.arcana.backend.table.ParticipantesAlteradosEvent;
import com.arcana.backend.table.model.StatusMesa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MesaChatHandlerTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final GerenciadorMesasAtivas gerenciador = new GerenciadorMesasAtivas();
    private final TaskScheduler agendador = mock(TaskScheduler.class);
    private MesaChatHandler handler;
    private WebSocketSession remetente;
    private WebSocketSession outroJogador;
    private WebSocketSession jogadorDeOutraMesa;

    private MesaChatHandler novoHandler(Clock relogio) {
        RandomGenerator sempreQuatro = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return 3;
            }
        };
        RolagemDadosService rolagens = new RolagemDadosService(sempreQuatro);
        DesafioChat desafioChat = new DesafioChat(
                new DesafioService(mock(MesaService.class), rolagens, relogio), objectMapper);
        SalaChat salaChat = new SalaChat(new SalaService(mock(MesaService.class), relogio), objectMapper);
        CombateChat combateChat = new CombateChat(new CombateService(mock(MesaService.class), rolagens, relogio), objectMapper);
        return new MesaChatHandler(objectMapper, rolagens, new ValidadorImagem(), desafioChat, salaChat, combateChat,
                gerenciador,
                agendador, relogio, Duration.ofSeconds(60));
    }

    @BeforeEach
    void setUp() throws Exception {
        handler = novoHandler(Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC));

        remetente = sessaoAutenticada("a", new AcessoMesaDto(1L, 2L, "joao", "JOGADOR", StatusMesa.ABERTA));
        outroJogador = sessaoAutenticada("b", new AcessoMesaDto(1L, 3L, "maria", "JOGADOR", StatusMesa.ABERTA));
        jogadorDeOutraMesa = sessaoAutenticada("c", new AcessoMesaDto(9L, 4L, "pedro", "JOGADOR", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(remetente);
        handler.afterConnectionEstablished(outroJogador);
        handler.afterConnectionEstablished(jogadorDeOutraMesa);
        clearInvocations(remetente, outroJogador, jogadorDeOutraMesa);
    }

    private WebSocketSession sessaoAutenticada(String id, AcessoMesaDto acesso) {
        Map<String, Object> atributos = new HashMap<>();
        if (acesso != null) {
            atributos.put(AutenticacaoHandshakeInterceptor.ATRIBUTO_ACESSO, acesso);
        }
        WebSocketSession sessao = mock(WebSocketSession.class);
        when(sessao.getId()).thenReturn(id);
        when(sessao.getAttributes()).thenReturn(atributos);
        when(sessao.isOpen()).thenReturn(true);
        return sessao;
    }

    private List<JsonNode> mensagensRecebidas(WebSocketSession sessao) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessao, atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues().stream()
                .map(m -> {
                    try {
                        return objectMapper.readTree(m.getPayload());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
    }

    @Test
    void deveIgnorarMensagensQuePassamDoLimiteDaJanela() throws Exception {
        String texto = "{\"type\":\"TEXTO\",\"content\":\"oi\"}";
        for (int i = 0; i < MesaChatHandler.LIMITE_MENSAGENS_NA_JANELA; i++) {
            handler.handleMessage(remetente, new TextMessage(texto));
        }
        clearInvocations(outroJogador);

        handler.handleMessage(remetente, new TextMessage(texto));

        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
        JsonNode aviso = mensagensRecebidas(remetente).get(mensagensRecebidas(remetente).size() - 1);
        assertEquals("SISTEMA", aviso.get("type").asText());
        assertTrue(aviso.get("content").asText().contains("rápido demais"));
    }

    @Test
    void deveRecusarConexaoAlemDoLimitePorUsuario() throws Exception {
        AcessoMesaDto acesso = new AcessoMesaDto(1L, 5L, "ana", "JOGADOR", StatusMesa.ABERTA);
        for (int i = 0; i < MesaChatHandler.LIMITE_CONEXOES_POR_USUARIO; i++) {
            handler.afterConnectionEstablished(sessaoAutenticada("ana" + i, acesso));
        }
        WebSocketSession excedente = sessaoAutenticada("ana-extra", acesso);

        handler.afterConnectionEstablished(excedente);

        verify(excedente).close(any(CloseStatus.class));
        assertEquals(MesaChatHandler.LIMITE_CONEXOES_POR_USUARIO,
                gerenciador.buscar(1L).orElseThrow().sessoesDoUsuario(5L).size());
    }

    @Test
    void deveSubstituirResultadoForjadoPeloCalculoDoServidor() throws Exception {
        String payload = """
                {"type":"ROLAGEM","content":"rolou",
                 "diceResult":{"expression":"1d20","rolls":[20],"modifier":0,"total":999},
                 "rollRequest":{"lados":20,"quantidade":1,"modificador":2,"modo":"NORMAL"}}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        JsonNode recebida = mensagensRecebidas(outroJogador).get(0);
        assertEquals(6, recebida.get("diceResult").get("total").asInt());
        assertEquals(4, recebida.get("diceResult").get("rolls").get(0).asInt());
        assertEquals("rolou 1d20+2", recebida.get("content").asText());
    }

    @Test
    void deveEnviarErroApenasParaQuemPediuRolagemInvalida() throws Exception {
        String payload = """
                {"type":"ROLAGEM","content":"",
                 "rollRequest":{"lados":7,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        List<JsonNode> doRemetente = mensagensRecebidas(remetente);
        assertEquals(1, doRemetente.size());
        assertEquals("SISTEMA", doRemetente.get(0).get("type").asText());
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveUsarAutorEMesaDaSessaoAutenticadaIgnorandoOCliente() throws Exception {
        String payload = """
                {"tableId":"9","authorId":"999","authorName":"Mestre Falso","type":"TEXTO",
                 "content":"olá","createdAt":"2000-01-01T00:00:00Z"}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        JsonNode recebida = mensagensRecebidas(outroJogador).get(0);
        assertEquals("1", recebida.get("tableId").asText());
        assertEquals("2", recebida.get("authorId").asText());
        assertEquals("joao", recebida.get("authorName").asText());
        assertNotEquals("2000-01-01T00:00:00Z", recebida.get("createdAt").asText());
        assertTrue(mensagensRecebidas(jogadorDeOutraMesa).isEmpty());
    }

    @Test
    void deveRecusarMensagemDeSistemaEnviadaPeloCliente() throws Exception {
        String payload = """
                {"type":"SISTEMA","content":"A mesa foi encerrada."}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        assertEquals("Tipo de mensagem não permitido.", mensagensRecebidas(remetente).get(0).get("content").asText());
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveFecharSessaoSemIdentidadeAutenticada() throws Exception {
        WebSocketSession anonima = sessaoAutenticada("x", null);

        handler.afterConnectionEstablished(anonima);

        verify(anonima).close(CloseStatus.POLICY_VIOLATION);
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    private void enviarTexto(WebSocketSession sessao, String conteudo) throws Exception {
        handler.handleMessage(sessao, new TextMessage(
                objectMapper.writeValueAsString(Map.of("type", "TEXTO", "content", conteudo))));
    }

    @Test
    void deveEnviarHistoricoEPresentesParaQuemConecta() throws Exception {
        enviarTexto(remetente, "a porta está trancada");

        WebSocketSession mestre = sessaoAutenticada("m", new AcessoMesaDto(1L, 1L, "gandalf", "MESTRE", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(mestre);

        JsonNode sincronizacao = mensagensRecebidas(mestre).get(0);
        assertEquals("HISTORICO", sincronizacao.get("type").asText());

        List<String> conteudos = new ArrayList<>();
        sincronizacao.get("historico").get("CONVERSA").forEach(m -> conteudos.add(m.get("content").asText()));
        assertTrue(conteudos.contains("a porta está trancada"));
        assertTrue(conteudos.contains("gandalf entrou na mesa."));

        JsonNode presentes = sincronizacao.get("presentes");
        assertEquals(3, presentes.size());
        assertEquals("gandalf", presentes.get(0).get("displayName").asText());
    }

    private static final String ROLAGEM_D20 = """
            {"type":"ROLAGEM","rollRequest":{"lados":20,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
            """;

    @Test
    void deveSepararTextoERolagemEmCanaisProprios() throws Exception {
        enviarTexto(remetente, "vou atacar");
        handler.handleMessage(remetente, new TextMessage(ROLAGEM_D20));

        List<JsonNode> recebidas = mensagensRecebidas(outroJogador);
        assertEquals("CONVERSA", recebidas.get(0).get("canal").asText());
        assertEquals("ROLAGENS", recebidas.get(1).get("canal").asText());

        MesaAtiva mesa = gerenciador.buscar(1L).orElseThrow();
        assertEquals(List.of("vou atacar"),
                mesa.historico(Canal.CONVERSA).stream()
                        .filter(m -> m.getType() == ChatMessage.Type.TEXTO).map(ChatMessage::getContent).toList());
        assertEquals(1, mesa.historico(Canal.ROLAGENS).size());
        assertEquals(ChatMessage.Type.ROLAGEM, mesa.historico(Canal.ROLAGENS).get(0).getType());
        assertTrue(mesa.historico(Canal.IMAGENS).isEmpty());
    }

    @Test
    void deveEnviarHistoricoSeparadoPorCanalParaQuemConecta() throws Exception {
        enviarTexto(remetente, "vou atacar");
        handler.handleMessage(remetente, new TextMessage(ROLAGEM_D20));

        WebSocketSession mestre = sessaoAutenticada("m", new AcessoMesaDto(1L, 1L, "gandalf", "MESTRE", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(mestre);

        JsonNode historico = mensagensRecebidas(mestre).get(0).get("historico");
        assertEquals(1, historico.get("ROLAGENS").size());
        assertEquals("ROLAGEM", historico.get("ROLAGENS").get(0).get("type").asText());
        assertEquals(0, historico.get("IMAGENS").size());
        historico.get("CONVERSA").forEach(m -> assertNotEquals("ROLAGEM", m.get("type").asText()));
    }

    @Test
    void deveRecusarMensagemEnviadaNoCanalErrado() throws Exception {
        handler.handleMessage(remetente, new TextMessage("""
                {"type":"TEXTO","canal":"ROLAGENS","content":"oi"}
                """));
        handler.handleMessage(remetente, new TextMessage("""
                {"type":"ROLAGEM","canal":"CONVERSA","rollRequest":{"lados":20,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
                """));

        List<JsonNode> doRemetente = mensagensRecebidas(remetente);
        assertEquals("Mensagens de texto só podem ser enviadas no canal de conversa.", doRemetente.get(0).get("content").asText());
        assertEquals("Rolagens só podem ser enviadas no canal de rolagens.", doRemetente.get(1).get("content").asText());
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
        assertTrue(gerenciador.buscar(1L).orElseThrow().historico(Canal.ROLAGENS).isEmpty());
    }

    private void enviarImagem(WebSocketSession sessao, byte[] conteudo) throws Exception {
        handler.handleMessage(sessao, new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "IMAGEM",
                "canal", "IMAGENS",
                "image", ValidadorImagemTest.comoDataUrl("image/png", conteudo)))));
    }

    @Test
    void deveTransmitirImagemValidaNoCanalDeImagens() throws Exception {
        enviarImagem(remetente, ValidadorImagemTest.PNG);

        JsonNode recebida = mensagensRecebidas(outroJogador).get(0);
        assertEquals("IMAGEM", recebida.get("type").asText());
        assertEquals("IMAGENS", recebida.get("canal").asText());
        assertEquals("image/png", recebida.get("image").get("mimeType").asText());
        assertEquals("joao", recebida.get("authorName").asText());
        assertEquals(1, gerenciador.buscar(1L).orElseThrow().historico(Canal.IMAGENS).size());
    }

    @Test
    void deveRecusarImagemInvalidaApenasParaQuemEnviou() throws Exception {
        enviarImagem(remetente, "não sou imagem".getBytes());

        assertTrue(mensagensRecebidas(remetente).get(0).get("content").asText().startsWith("Formato não suportado"));
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
        assertTrue(gerenciador.buscar(1L).orElseThrow().historico(Canal.IMAGENS).isEmpty());
    }

    @Test
    void deveGuardarApenasAsUltimasImagensDaSessao() throws Exception {
        for (int i = 0; i < MesaAtiva.LIMITE_IMAGENS + 3; i++) {
            enviarImagem(remetente, ValidadorImagemTest.PNG);
        }

        assertEquals(MesaAtiva.LIMITE_IMAGENS, gerenciador.buscar(1L).orElseThrow().historico(Canal.IMAGENS).size());
    }

    @Test
    void deveEnviarImagensDaSessaoSeparadasDoHistoricoParaQuemConecta() throws Exception {
        enviarImagem(remetente, ValidadorImagemTest.PNG);
        enviarImagem(remetente, ValidadorImagemTest.PNG);

        WebSocketSession mestre = sessaoAutenticada("m", new AcessoMesaDto(1L, 1L, "gandalf", "MESTRE", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(mestre);

        List<JsonNode> recebidas = mensagensRecebidas(mestre);
        assertEquals("HISTORICO", recebidas.get(0).get("type").asText());
        assertEquals(0, recebidas.get(0).get("historico").get("IMAGENS").size());
        assertEquals("IMAGEM", recebidas.get(1).get("type").asText());
        assertEquals("IMAGEM", recebidas.get(2).get("type").asText());
    }

    @Test
    void deveBloquearImagemComAMesaPausada() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));
        clearInvocations(remetente, outroJogador);

        enviarImagem(remetente, ValidadorImagemTest.PNG);

        assertTrue(mensagensRecebidas(remetente).get(0).get("content").asText().startsWith("A mesa está pausada."));
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveAvisarOsDemaisQuandoAlguemEntraOuSai() throws Exception {
        WebSocketSession novoJogador = sessaoAutenticada("n", new AcessoMesaDto(1L, 5L, "ana", "JOGADOR", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(novoJogador);

        List<JsonNode> aoEntrar = mensagensRecebidas(outroJogador);
        assertEquals("ana entrou na mesa.", aoEntrar.get(0).get("content").asText());
        assertEquals("PRESENCA", aoEntrar.get(1).get("type").asText());
        assertEquals(3, aoEntrar.get(1).get("presentes").size());
        clearInvocations(outroJogador);

        handler.afterConnectionClosed(novoJogador, CloseStatus.NORMAL);

        List<JsonNode> aoSair = mensagensRecebidas(outroJogador);
        assertEquals("ana saiu da mesa.", aoSair.get(0).get("content").asText());
        assertEquals(2, aoSair.get(1).get("presentes").size());
    }

    @Test
    void naoDeveRepetirAvisoQuandoMesmoUsuarioAbreOutraAba() throws Exception {
        WebSocketSession segundaAba = sessaoAutenticada("a2", new AcessoMesaDto(1L, 2L, "joao", "JOGADOR", StatusMesa.ABERTA));

        handler.afterConnectionEstablished(segundaAba);
        handler.afterConnectionClosed(segundaAba, CloseStatus.NORMAL);

        List<String> tipos = mensagensRecebidas(outroJogador).stream().map(m -> m.get("type").asText()).toList();
        assertEquals(List.of("PRESENCA", "PRESENCA"), tipos);
    }

    @Test
    void deveRemoverMesaAtivaQuandoUltimoParticipanteSai() {
        handler.afterConnectionClosed(remetente, CloseStatus.NORMAL);
        handler.afterConnectionClosed(outroJogador, CloseStatus.NORMAL);

        assertTrue(gerenciador.buscar(1L).isEmpty());
        assertTrue(gerenciador.buscar(9L).isPresent());
    }

    @Test
    void deveRecusarTextoVazioOuAcimaDoLimite() throws Exception {
        enviarTexto(remetente, "   ");
        enviarTexto(remetente, "a".repeat(MesaChatHandler.LIMITE_CARACTERES_TEXTO + 1));

        List<JsonNode> doRemetente = mensagensRecebidas(remetente);
        assertEquals(2, doRemetente.size());
        assertEquals("A mensagem está vazia.", doRemetente.get(0).get("content").asText());
        assertTrue(doRemetente.get(1).get("content").asText().contains("limite"));
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveIgnorarHistoricoForjadoPeloCliente() throws Exception {
        String payload = """
                {"type":"TEXTO","content":"oi",
                 "historico":{"CONVERSA":[{"type":"SISTEMA","content":"falso"}]},
                 "presentes":[{"userId":99,"displayName":"fantasma","role":"MESTRE"}]}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        JsonNode recebida = mensagensRecebidas(outroJogador).get(0);
        assertTrue(recebida.get("historico").isNull());
        assertTrue(recebida.get("presentes").isNull());
    }

    @Test
    void deveManterApenasAsUltimasMensagensNoHistorico() throws Exception {
        AtomicLong segundos = new AtomicLong();
        Clock relogioQueAvanca = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.parse("2026-10-29T15:00:00Z").plusSeconds(segundos.addAndGet(10));
            }
        };
        handler = novoHandler(relogioQueAvanca);
        for (int i = 0; i < MesaAtiva.LIMITE_HISTORICO + 10; i++) {
            enviarTexto(remetente, "mensagem " + i);
        }

        List<ChatMessage> historico = gerenciador.buscar(1L).orElseThrow().historico(Canal.CONVERSA);
        assertEquals(MesaAtiva.LIMITE_HISTORICO, historico.size());
        assertEquals("mensagem " + (MesaAtiva.LIMITE_HISTORICO + 9), historico.get(historico.size() - 1).getContent());
    }

    private WebSocketSession conectarMestre(String sessionId) throws Exception {
        WebSocketSession mestre = sessaoAutenticada(sessionId, new AcessoMesaDto(1L, 1L, "gandalf", "MESTRE", StatusMesa.ABERTA));
        handler.afterConnectionEstablished(mestre);
        return mestre;
    }

    private Runnable capturarEncerramentoAgendado(ScheduledFuture<?> futuro) {
        ArgumentCaptor<Runnable> tarefa = ArgumentCaptor.forClass(Runnable.class);
        verify(agendador).schedule(tarefa.capture(), eq(Instant.parse("2026-10-29T15:01:00Z")));
        return tarefa.getValue();
    }

    @Test
    void deveEncerrarSessaoQuandoMestreNaoVoltaDentroDaTolerancia() throws Exception {
        ScheduledFuture<?> futuro = mock(ScheduledFuture.class);
        doReturn(futuro).when(agendador).schedule(any(Runnable.class), any(Instant.class));
        WebSocketSession mestre = conectarMestre("m");
        clearInvocations(outroJogador);

        handler.afterConnectionClosed(mestre, CloseStatus.GOING_AWAY);

        assertTrue(mensagensRecebidas(outroJogador).get(0).get("content").asText()
                .contains("será encerrada em 60 segundos"));
        clearInvocations(remetente, outroJogador);

        capturarEncerramentoAgendado(futuro).run();

        assertEquals("SESSAO_ENCERRADA", mensagensRecebidas(outroJogador).get(0).get("type").asText());
        verify(remetente).close(any(CloseStatus.class));
        verify(outroJogador).close(any(CloseStatus.class));
        assertTrue(gerenciador.buscar(1L).isEmpty());
        assertTrue(gerenciador.buscar(9L).isPresent());
    }

    @Test
    void deveManterSessaoQuandoMestreVoltaDentroDaTolerancia() throws Exception {
        ScheduledFuture<?> futuro = mock(ScheduledFuture.class);
        doReturn(futuro).when(agendador).schedule(any(Runnable.class), any(Instant.class));
        WebSocketSession mestre = conectarMestre("m");
        handler.afterConnectionClosed(mestre, CloseStatus.GOING_AWAY);
        Runnable encerramento = capturarEncerramentoAgendado(futuro);
        clearInvocations(outroJogador);

        conectarMestre("m2");

        verify(futuro).cancel(false);
        assertEquals("O mestre gandalf voltou. A sessão continua.",
                mensagensRecebidas(outroJogador).get(0).get("content").asText());

        encerramento.run();

        assertTrue(gerenciador.buscar(1L).isPresent());
        verify(outroJogador, never()).close(any(CloseStatus.class));
    }

    @Test
    void naoDeveAgendarEncerramentoQuandoJogadorSai() {
        handler.afterConnectionClosed(remetente, CloseStatus.NORMAL);

        verifyNoInteractions(agendador);
    }

    @Test
    void deveEncerrarSessaoImediatamenteQuandoMestreEncerraAMesa() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.ENCERRADA));

        JsonNode evento = mensagensRecebidas(outroJogador).get(0);
        assertEquals("SESSAO_ENCERRADA", evento.get("type").asText());
        assertEquals("O mestre encerrou a mesa.", evento.get("content").asText());
        verify(remetente).close(any(CloseStatus.class));
        assertTrue(gerenciador.buscar(1L).isEmpty());
        verify(jogadorDeOutraMesa, never()).close(any(CloseStatus.class));
    }

    @Test
    void deveAvisarAMesaQuandoOMestrePausa() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));

        List<JsonNode> recebidas = mensagensRecebidas(outroJogador);
        assertEquals("O mestre pausou a mesa. Mensagens e rolagens estão bloqueadas.",
                recebidas.get(0).get("content").asText());
        assertEquals("STATUS_MESA", recebidas.get(1).get("type").asText());
        assertEquals("PAUSADA", recebidas.get(1).get("content").asText());
        assertTrue(mensagensRecebidas(jogadorDeOutraMesa).isEmpty());
    }

    @Test
    void deveBloquearMensagensERolagensEnquantoAMesaEstaPausada() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));
        clearInvocations(remetente, outroJogador);

        enviarTexto(remetente, "alguém aí?");
        handler.handleMessage(remetente, new TextMessage("""
                {"type":"ROLAGEM","rollRequest":{"lados":20,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
                """));

        List<JsonNode> doRemetente = mensagensRecebidas(remetente);
        assertEquals(2, doRemetente.size());
        assertTrue(doRemetente.get(0).get("content").asText().startsWith("A mesa está pausada."));
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveLiberarMensagensQuandoOMestreRetomaAMesa() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.EM_ANDAMENTO));
        clearInvocations(outroJogador);

        enviarTexto(remetente, "voltamos");

        assertEquals("voltamos", mensagensRecebidas(outroJogador).get(0).get("content").asText());
        List<String> historico = gerenciador.buscar(1L).orElseThrow().historico(Canal.CONVERSA).stream()
                .map(ChatMessage::getContent).toList();
        assertTrue(historico.contains("O mestre retomou a mesa."));
    }

    @Test
    void deveDesconectarJogadorRemovidoPeloMestreEAvisarOsDemais() throws Exception {
        handler.aoRemoverParticipante(new ParticipanteRemovidoEvent(1L, 2L, "joao", true));

        JsonNode aviso = mensagensRecebidas(remetente).get(0);
        assertEquals("REMOVIDO_DA_MESA", aviso.get("type").asText());
        assertEquals("O mestre removeu você da mesa.", aviso.get("content").asText());
        verify(remetente).close(any(CloseStatus.class));

        List<JsonNode> recebidas = mensagensRecebidas(outroJogador);
        assertEquals("O mestre removeu joao da mesa.", recebidas.get(0).get("content").asText());
        assertEquals("PARTICIPANTES", recebidas.get(1).get("type").asText());
        assertEquals(1, recebidas.get(2).get("presentes").size());
        assertTrue(mensagensRecebidas(jogadorDeOutraMesa).isEmpty());
        clearInvocations(outroJogador);

        handler.afterConnectionClosed(remetente, CloseStatus.NORMAL);

        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }

    @Test
    void deveAvisarQuandoJogadorDeixaAMesa() throws Exception {
        handler.aoRemoverParticipante(new ParticipanteRemovidoEvent(1L, 2L, "joao", false));

        assertEquals("Você saiu da mesa.", mensagensRecebidas(remetente).get(0).get("content").asText());
        assertEquals("joao deixou a mesa.", mensagensRecebidas(outroJogador).get(0).get("content").asText());
        assertFalse(gerenciador.buscar(1L).orElseThrow().usuarioConectado(2L));
    }

    @Test
    void deveAvisarApenasAMesaQuandoOsParticipantesMudam() throws Exception {
        handler.aoAlterarParticipantes(new ParticipantesAlteradosEvent(1L));

        assertEquals("PARTICIPANTES", mensagensRecebidas(outroJogador).get(0).get("type").asText());
        assertTrue(mensagensRecebidas(jogadorDeOutraMesa).isEmpty());
    }

    @Test
    void deveBloquearMensagensAoConectarEmMesaJaPausada() throws Exception {
        WebSocketSession jogador = sessaoAutenticada("p", new AcessoMesaDto(7L, 2L, "joao", "JOGADOR", StatusMesa.PAUSADA));
        WebSocketSession colega = sessaoAutenticada("q", new AcessoMesaDto(7L, 3L, "maria", "JOGADOR", StatusMesa.PAUSADA));
        handler.afterConnectionEstablished(jogador);
        handler.afterConnectionEstablished(colega);
        clearInvocations(jogador, colega);

        enviarTexto(jogador, "olá");

        assertTrue(mensagensRecebidas(jogador).get(0).get("content").asText().startsWith("A mesa está pausada."));
        assertTrue(mensagensRecebidas(colega).isEmpty());
    }
}
