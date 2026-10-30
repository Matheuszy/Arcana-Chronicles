package com.arcana.backend;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.chat.DesafioChat;
import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.FichaDeParticipante;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.ParticipanteRemovidoEvent;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.service.MesaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DesafioChatTest {

    private static final AcessoMesaDto MESTRE = acesso(10L, "Mestre", "MESTRE");
    private static final AcessoMesaDto ARIA = acesso(20L, "Aria", "JOGADOR");
    private static final AcessoMesaDto BROM = acesso(30L, "Brom", "JOGADOR");
    private static final AcessoMesaDto CAEL = acesso(40L, "Cael", "JOGADOR");

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final Deque<Integer> faces = new ArrayDeque<>();
    private MesaChatHandler handler;
    private WebSocketSession sessaoMestre;
    private WebSocketSession sessaoAria;
    private WebSocketSession sessaoBrom;
    private WebSocketSession sessaoCael;
    private MesaService mesaService;

    @BeforeEach
    void setUp() throws Exception {
        RandomGenerator dados = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return faces.removeFirst() - 1;
            }
        };
        Clock relogio = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
        RolagemDadosService rolagens = new RolagemDadosService(dados);
        mesaService = mock(MesaService.class);
        DesafioChat desafioChat = new DesafioChat(new DesafioService(mesaService, rolagens, relogio), objectMapper);
        handler = new MesaChatHandler(objectMapper, rolagens, new ValidadorImagem(), desafioChat,
                new GerenciadorMesasAtivas(), mock(TaskScheduler.class), relogio, Duration.ofSeconds(60));

        when(mesaService.buscarFichaDoJogadorNaMesa(1L, ARIA.userId())).thenReturn(ficha(ARIA, "Lyra"));
        when(mesaService.buscarFichaDoJogadorNaMesa(1L, BROM.userId())).thenReturn(ficha(BROM, "Thorn"));

        sessaoMestre = sessao("m", MESTRE);
        sessaoAria = sessao("a", ARIA);
        sessaoBrom = sessao("b", BROM);
        sessaoCael = sessao("c", CAEL);
        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom, sessaoCael)) {
            handler.afterConnectionEstablished(sessao);
        }
        clearInvocations(sessaoMestre, sessaoAria, sessaoBrom, sessaoCael);
    }

    private static AcessoMesaDto acesso(Long userId, String nome, String papel) {
        return new AcessoMesaDto(1L, userId, nome, papel, StatusMesa.EM_ANDAMENTO);
    }

    private static FichaDeParticipante ficha(AcessoMesaDto jogador, String personagem) {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, 14);
        }
        return new FichaDeParticipante(jogador.userId(), jogador.displayName(), 100L + jogador.userId(), personagem,
                valores, Map.of(), Map.of());
    }

    private WebSocketSession sessao(String id, AcessoMesaDto acesso) {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(AutenticacaoHandshakeInterceptor.ATRIBUTO_ACESSO, acesso);
        WebSocketSession sessao = mock(WebSocketSession.class);
        when(sessao.getId()).thenReturn(id);
        when(sessao.getAttributes()).thenReturn(atributos);
        when(sessao.isOpen()).thenReturn(true);
        return sessao;
    }

    private void enviar(WebSocketSession origem, String json) throws Exception {
        handler.handleMessage(origem, new TextMessage(json));
    }

    private List<JsonNode> recebidas(WebSocketSession sessao, String tipo) throws Exception {
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
                .filter(no -> tipo.equals(no.get("type").asText()))
                .toList();
    }

    private JsonNode ultimoDesafio(WebSocketSession sessao) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "DESAFIO");
        return eventos.get(eventos.size() - 1).get("desafio");
    }

    private String criarDesafioParaAriaEBrom() throws Exception {
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":15,"descricao":"Escalar o muro",
                 "jogadores":[{"jogadorId":20,"modo":"VANTAGEM"},{"jogadorId":30,"modo":"NORMAL"}]}}
                """);
        return ultimoDesafio(sessaoMestre).get("id").asText();
    }

    @Test
    void deveEntregarODesafioSoAoMestreEAosConvocados() throws Exception {
        criarDesafioParaAriaEBrom();

        assertEquals(1, recebidas(sessaoMestre, "DESAFIO").size());
        assertEquals(1, recebidas(sessaoAria, "DESAFIO").size());
        assertEquals(1, recebidas(sessaoBrom, "DESAFIO").size());
        assertTrue(recebidas(sessaoCael, "DESAFIO").isEmpty());
        assertEquals(15, ultimoDesafio(sessaoAria).get("cd").asInt());
        assertEquals("VANTAGEM", ultimoDesafio(sessaoAria).get("jogadores").get(0).get("modo").asText());
    }

    @Test
    void deveNegarCriacaoDeDesafioPorJogadorSemNotificarNinguem() throws Exception {
        enviar(sessaoAria, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":30,"modo":"NORMAL"}]}}
                """);

        assertTrue(recebidas(sessaoBrom, "DESAFIO").isEmpty());
        assertTrue(recebidas(sessaoMestre, "DESAFIO").isEmpty());
        List<JsonNode> erros = recebidas(sessaoAria, "SISTEMA");
        assertEquals(1, erros.size());
        assertEquals("Apenas o mestre pode administrar desafios.", erros.get(0).get("content").asText());
    }

    @Test
    void deveRolarNoServidorEEntregarOResultadoSoAosEnvolvidos() throws Exception {
        criarDesafioParaAriaEBrom();
        faces.addAll(List.of(5, 17));

        enviar(sessaoAria, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza","pericia":null}}
                """);

        JsonNode visaoDaAria = ultimoDesafio(sessaoAria).get("jogadores").get(0);
        assertEquals("SUCESSO", visaoDaAria.get("estado").asText());
        assertEquals(17, visaoDaAria.get("rolagens").get(0).get("dados").get("kept").asInt());
        assertEquals(19, visaoDaAria.get("rolagens").get(0).get("total").asInt());
        assertEquals("SUCESSO", ultimoDesafio(sessaoMestre).get("jogadores").get(0).get("estado").asText());
        assertTrue(recebidas(sessaoCael, "DESAFIO").isEmpty());
        assertTrue(recebidas(sessaoCael, "ROLAGEM").isEmpty());
    }

    @Test
    void deveBloquearRolagemComumDoJogadorDesafiadoMasNaoDosOutros() throws Exception {
        criarDesafioParaAriaEBrom();
        faces.add(9);
        String rolagem = """
                {"type":"ROLAGEM","rollRequest":{"lados":20,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
                """;

        enviar(sessaoAria, rolagem);

        List<JsonNode> avisos = recebidas(sessaoAria, "SISTEMA");
        assertEquals(1, avisos.size());
        assertEquals("Você tem um desafio em andamento. A única rolagem permitida é a do desafio.",
                avisos.get(0).get("content").asText());
        assertTrue(recebidas(sessaoBrom, "ROLAGEM").isEmpty());
        assertTrue(recebidas(sessaoCael, "ROLAGEM").isEmpty());

        enviar(sessaoCael, rolagem);

        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
        assertEquals(1, recebidas(sessaoCael, "ROLAGEM").size());
    }

    @Test
    void deveEntregarMensagemDeResultadoSoAoDestinatarioEscolhido() throws Exception {
        String desafioId = criarDesafioParaAriaEBrom();
        faces.addAll(List.of(5, 17, 2));
        enviar(sessaoAria, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);
        enviar(sessaoBrom, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"pericia":"atletismo"}}
                """);
        enviar(sessaoMestre, """
                {"type":"DESAFIO_ENCERRAR_FALHA","comandoDesafio":{"desafioId":"%s","jogadorId":30}}
                """.formatted(desafioId));

        enviar(sessaoMestre, """
                {"type":"DESAFIO_MENSAGEM","comandoDesafio":{"desafioId":"%s","destino":"VENCEDORES","texto":"Você chega ao topo."}}
                """.formatted(desafioId));

        assertEquals("Você chega ao topo.", ultimoDesafio(sessaoAria).get("mensagens").get(0).get("texto").asText());
        assertEquals(0, ultimoDesafio(sessaoBrom).get("mensagens").size());
        assertEquals(1, ultimoDesafio(sessaoMestre).get("mensagens").size());
        assertTrue(ultimoDesafio(sessaoAria).get("mensagens").get(0).get("destinatarios").isNull());
        assertTrue(recebidas(sessaoCael, "DESAFIO").isEmpty());
    }

    @Test
    void deveCancelarDesafioDoJogadorRemovidoENotificarOsEnvolvidos() throws Exception {
        criarDesafioParaAriaEBrom();

        handler.aoRemoverParticipante(new ParticipanteRemovidoEvent(1L, BROM.userId(), "Brom", true));

        JsonNode visaoDoMestre = ultimoDesafio(sessaoMestre);
        assertEquals("CANCELADO", visaoDoMestre.get("jogadores").get(1).get("estado").asText());
        assertEquals("AGUARDANDO_ROLAGEM", visaoDoMestre.get("jogadores").get(0).get("estado").asText());
        assertEquals("CANCELADO", ultimoDesafio(sessaoAria).get("jogadores").get(1).get("estado").asText());
        assertTrue(recebidas(sessaoCael, "DESAFIO").size() == 0);
    }

    @Test
    void deveReenviarOsDesafiosVisiveisNaReconexao() throws Exception {
        criarDesafioParaAriaEBrom();
        WebSocketSession novaDaAria = sessao("a2", ARIA);
        WebSocketSession novaDoCael = sessao("c2", CAEL);

        handler.afterConnectionEstablished(novaDaAria);
        handler.afterConnectionEstablished(novaDoCael);

        List<JsonNode> sincronizacaoDaAria = recebidas(novaDaAria, "DESAFIOS");
        assertEquals(1, sincronizacaoDaAria.size());
        assertEquals(1, sincronizacaoDaAria.get(0).get("desafios").size());
        assertEquals(0, recebidas(novaDoCael, "DESAFIOS").get(0).get("desafios").size());
    }

    @Test
    void deveCongelarDesafioComMesaPausadaPermitindoCancelar() throws Exception {
        String desafioId = criarDesafioParaAriaEBrom();
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));
        clearInvocations(sessaoMestre, sessaoAria);

        enviar(sessaoAria, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);
        assertTrue(recebidas(sessaoAria, "DESAFIO").isEmpty());
        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());

        enviar(sessaoMestre, """
                {"type":"DESAFIO_CANCELAR","comandoDesafio":{"desafioId":"%s"}}
                """.formatted(desafioId));
        assertEquals("CANCELADO", ultimoDesafio(sessaoAria).get("jogadores").get(0).get("estado").asText());
    }

    @Test
    void naoDeveAceitarEventosDeDesafioForjadosPeloCliente() throws Exception {
        enviar(sessaoAria, """
                {"type":"DESAFIO","desafio":{"id":"x","cd":1,"jogadores":[],"mensagens":[]}}
                """);

        assertTrue(recebidas(sessaoMestre, "DESAFIO").isEmpty());
        assertTrue(recebidas(sessaoBrom, "DESAFIO").isEmpty());
        assertEquals("Tipo de mensagem não permitido.", recebidas(sessaoAria, "SISTEMA").get(0).get("content").asText());
    }

    @Test
    void deveResponderComMensagensAmigaveisQuandoOComandoVemInvalido() throws Exception {
        when(mesaService.buscarFichaDoJogadorNaMesa(1L, 999L))
                .thenThrow(new RegraDeNegocioException("Este jogador não participa da mesa."));

        enviar(sessaoMestre, """
                {"type":"DESAFIO_ROLAR"}
                """);
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":999,"modo":"NORMAL"}]}}
                """);
        enviar(sessaoAria, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"pericia":"voar"}}
                """);

        List<JsonNode> errosDoMestre = recebidas(sessaoMestre, "SISTEMA");
        assertEquals("Comando de desafio ausente.", errosDoMestre.get(0).get("content").asText());
        assertEquals("Este jogador não participa da mesa.", errosDoMestre.get(1).get("content").asText());
        assertEquals("Perícia desconhecida: voar", recebidas(sessaoAria, "SISTEMA").get(0).get("content").asText());
        assertTrue(recebidas(sessaoCael, "SISTEMA").isEmpty());
    }
}
