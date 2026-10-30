package com.arcana.backend;

import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.chat.DesafioChat;
import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.SalaChat;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.sala.SalaService;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.AlvoDeStatus;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalaChatTest {

    private static final AcessoMesaDto MESTRE = acesso(10L, "Mestre", "MESTRE");
    private static final AcessoMesaDto ARIA = acesso(20L, "Aria", "JOGADOR");
    private static final AcessoMesaDto BROM = acesso(30L, "Brom", "JOGADOR");

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private MesaChatHandler handler;
    private MesaService mesaService;
    private WebSocketSession sessaoMestre;
    private WebSocketSession sessaoAria;
    private WebSocketSession sessaoAriaOutraAba;
    private WebSocketSession sessaoBrom;

    @BeforeEach
    void setUp() throws Exception {
        Clock relogio = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
        RolagemDadosService rolagens = new RolagemDadosService(new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }
        });
        mesaService = mock(MesaService.class);
        DesafioChat desafioChat = new DesafioChat(new DesafioService(mesaService, rolagens, relogio), objectMapper);
        SalaChat salaChat = new SalaChat(new SalaService(mesaService, relogio), objectMapper);
        handler = new MesaChatHandler(objectMapper, rolagens, new ValidadorImagem(), desafioChat, salaChat,
                new GerenciadorMesasAtivas(), mock(TaskScheduler.class), relogio, Duration.ofSeconds(60));

        when(mesaService.buscarAlvoDeStatus(1L, 100L)).thenReturn(new AlvoDeStatus(100L, "Thorn", false, 20L));
        when(mesaService.buscarAlvoDeStatus(1L, 200L)).thenReturn(new AlvoDeStatus(200L, "Goblin", true, 10L));
        when(mesaService.buscarAlvoDeStatus(1L, 300L))
                .thenThrow(new RegraDeNegocioException("Esta ficha não está em uso nesta mesa."));

        sessaoMestre = sessao("m", MESTRE);
        sessaoAria = sessao("a", ARIA);
        sessaoAriaOutraAba = sessao("a2", ARIA);
        sessaoBrom = sessao("b", BROM);
        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoAriaOutraAba, sessaoBrom)) {
            handler.afterConnectionEstablished(sessao);
        }
        clearInvocations(sessaoMestre, sessaoAria, sessaoAriaOutraAba, sessaoBrom);
    }

    private static AcessoMesaDto acesso(Long userId, String nome, String papel) {
        return new AcessoMesaDto(1L, userId, nome, papel, StatusMesa.EM_ANDAMENTO);
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

    private JsonNode ultima(WebSocketSession sessao, String tipo) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, tipo);
        return eventos.get(eventos.size() - 1);
    }

    private WebSocketSession novaConexao(String id, AcessoMesaDto acesso) throws Exception {
        WebSocketSession sessao = sessao(id, acesso);
        handler.afterConnectionEstablished(sessao);
        return sessao;
    }

    @Test
    void deveEntregarANotaSoAoDonoEmTodasAsAbasDele() throws Exception {
        enviar(sessaoAria, "{\"type\":\"NOTA_SALVAR\",\"content\":\"o cofre está no porão\"}");

        assertEquals("o cofre está no porão", ultima(sessaoAria, "NOTA").get("content").asText());
        assertEquals("o cofre está no porão", ultima(sessaoAriaOutraAba, "NOTA").get("content").asText());
        assertTrue(recebidas(sessaoBrom, "NOTA").isEmpty());
        assertTrue(recebidas(sessaoMestre, "NOTA").isEmpty());
    }

    @Test
    void deveRestaurarANotaNaReconexaoSemMostrarAOutrasPessoas() throws Exception {
        enviar(sessaoAria, "{\"type\":\"NOTA_SALVAR\",\"content\":\"minha nota\"}");

        WebSocketSession reconectada = novaConexao("a3", ARIA);
        WebSocketSession deBrom = novaConexao("b2", BROM);

        assertEquals("minha nota", ultima(reconectada, "NOTA").get("content").asText());
        assertEquals("", ultima(deBrom, "NOTA").get("content").asText());
    }

    @Test
    void deveRecusarNotaAcimaDoLimiteSoParaQuemEnviou() throws Exception {
        enviar(sessaoAria, "{\"type\":\"NOTA_SALVAR\",\"content\":\"" + "x".repeat(2_001) + "\"}");

        assertTrue(recebidas(sessaoAria, "NOTA").isEmpty());
        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());
        assertTrue(recebidas(sessaoBrom, "SISTEMA").isEmpty());
    }

    @Test
    void deveAnunciarStatusDeJogadorNoChatEAtualizarOCanalDeTodos() throws Exception {
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":100,\"texto\":\"Envenenado\"}}");

        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom)) {
            List<JsonNode> avisos = recebidas(sessao, "SISTEMA");
            assertEquals(1, avisos.size());
            assertEquals("Status de Thorn: Envenenado.", avisos.get(0).get("content").asText());
            assertEquals("Envenenado", ultima(sessao, "STATUS").get("status").get("atuais").get(0).get("texto").asText());
        }
    }

    @Test
    void naoDeveRevelarStatusDeNpcAJogadoresNemEscreverNoChat() throws Exception {
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":200,\"texto\":\"Furioso\"}}");

        assertEquals(1, ultima(sessaoMestre, "STATUS").get("status").get("atuais").size());
        assertTrue(recebidas(sessaoMestre, "SISTEMA").isEmpty());
        for (WebSocketSession sessao : List.of(sessaoAria, sessaoAriaOutraAba, sessaoBrom)) {
            assertTrue(recebidas(sessao, "SISTEMA").isEmpty());
            assertEquals(0, ultima(sessao, "STATUS").get("status").get("atuais").size());
            assertEquals(0, ultima(sessao, "STATUS").get("status").get("historico").size());
        }
    }

    @Test
    void deveRecusarStatusDeJogadorEDeFichaForaDaMesa() throws Exception {
        enviar(sessaoAria, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":100,\"texto\":\"Herói\"}}");
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":300,\"texto\":\"Algo\"}}");

        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());
        assertEquals(1, recebidas(sessaoMestre, "SISTEMA").size());
        assertTrue(recebidas(sessaoBrom, "SISTEMA").isEmpty());
        assertTrue(recebidas(sessaoBrom, "STATUS").isEmpty());
    }

    @Test
    void deveAnunciarRemocaoDeStatusEManterHistoricoNoCanal() throws Exception {
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":100,\"texto\":\"Envenenado\"}}");
        enviar(sessaoMestre, "{\"type\":\"STATUS_REMOVER\",\"comandoSala\":{\"personagemId\":100}}");

        JsonNode status = ultima(sessaoBrom, "STATUS").get("status");
        assertEquals(0, status.get("atuais").size());
        assertEquals(2, status.get("historico").size());
        assertEquals("REMOVIDO", status.get("historico").get(1).get("acao").asText());
        assertEquals("Status de Thorn removido (Envenenado).", ultima(sessaoBrom, "SISTEMA").get("content").asText());
    }

    @Test
    void deveLimparOStatusDoJogadorRemovidoDaMesa() throws Exception {
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":100,\"texto\":\"Envenenado\"}}");

        handler.aoRemoverParticipante(new ParticipanteRemovidoEvent(1L, ARIA.userId(), "Aria", true));

        assertEquals(0, ultima(sessaoBrom, "STATUS").get("status").get("atuais").size());
    }

    @Test
    void deveSincronizarStatusParaQuemConectaDepois() throws Exception {
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":100,\"texto\":\"Envenenado\"}}");
        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":200,\"texto\":\"Furioso\"}}");

        JsonNode doMestre = ultima(novaConexao("m2", MESTRE), "STATUS").get("status");
        JsonNode doJogador = ultima(novaConexao("b2", BROM), "STATUS").get("status");

        assertEquals(2, doMestre.get("atuais").size());
        assertEquals(1, doJogador.get("atuais").size());
    }

    @Test
    void deveCompartilharItemComTodosEPermitirRemocaoSoAoAutorOuAoMestre() throws Exception {
        enviar(sessaoAria, "{\"type\":\"ITEM_CRIAR\",\"comandoSala\":{\"nome\":\"Chave de ferro\",\"descricao\":\"Abre o porão\"}}");

        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoAriaOutraAba, sessaoBrom)) {
            JsonNode itens = ultima(sessao, "ITENS").get("itens");
            assertEquals(1, itens.size());
            assertEquals("Chave de ferro", itens.get(0).get("nome").asText());
            assertEquals("Aria", itens.get(0).get("autorNome").asText());
        }

        String itemId = ultima(sessaoBrom, "ITENS").get("itens").get(0).get("id").asText();
        enviar(sessaoBrom, "{\"type\":\"ITEM_REMOVER\",\"comandoSala\":{\"itemId\":\"" + itemId + "\"}}");
        assertEquals(1, recebidas(sessaoBrom, "SISTEMA").size());
        assertEquals(1, ultima(sessaoAria, "ITENS").get("itens").size());

        enviar(sessaoMestre, "{\"type\":\"ITEM_REMOVER\",\"comandoSala\":{\"itemId\":\"" + itemId + "\"}}");
        assertEquals(0, ultima(sessaoBrom, "ITENS").get("itens").size());
    }

    @Test
    void deveRecusarItemSemNomeSemNotificarOutrasPessoas() throws Exception {
        enviar(sessaoAria, "{\"type\":\"ITEM_CRIAR\",\"comandoSala\":{\"nome\":\"  \",\"descricao\":\"sem nome\"}}");

        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());
        assertTrue(recebidas(sessaoBrom, "ITENS").isEmpty());
    }

    @Test
    void deveBloquearCriacaoDeItemComAMesaPausada() throws Exception {
        handler.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA));
        clearInvocations(sessaoAria, sessaoBrom);

        enviar(sessaoAria, "{\"type\":\"ITEM_CRIAR\",\"comandoSala\":{\"nome\":\"Adaga\"}}");

        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());
        assertTrue(recebidas(sessaoBrom, "ITENS").isEmpty());
    }

    @Test
    void deveEntregarItensJaCriadosAQuemConectaDepois() throws Exception {
        enviar(sessaoAria, "{\"type\":\"ITEM_CRIAR\",\"comandoSala\":{\"nome\":\"Adaga\"}}");

        assertEquals(1, ultima(novaConexao("b2", BROM), "ITENS").get("itens").size());
    }

    @Test
    void deveResponderComMensagemAmigavelAJsonInvalidoSemFecharAConexao() throws Exception {
        enviar(sessaoAria, "isso não é json");

        List<JsonNode> avisos = recebidas(sessaoAria, "SISTEMA");
        assertEquals(1, avisos.size());
        assertEquals("Não foi possível entender essa mensagem.", avisos.get(0).get("content").asText());
        verify(sessaoAria, never()).close(any());
        assertTrue(recebidas(sessaoBrom, "SISTEMA").isEmpty());
    }

    @Test
    void deveEsconderDetalhesTecnicosDeFalhasInesperadasDaSala() throws Exception {
        when(mesaService.buscarAlvoDeStatus(1L, 400L)).thenThrow(new IllegalStateException("conexão com o banco caiu"));
        when(mesaService.buscarAlvoDeStatus(1L, 500L)).thenThrow(new NullPointerException("detalhe interno"));

        enviar(sessaoMestre, "{\"type\":\"STATUS_DEFINIR\",\"comandoSala\":{\"personagemId\":500,\"texto\":\"x\"}}");

        JsonNode aviso = ultima(sessaoMestre, "SISTEMA");
        assertEquals("Não foi possível concluir a ação. Tente novamente.", aviso.get("content").asText());
        assertFalse(aviso.get("content").asText().contains("detalhe interno"));
    }

    @Test
    void deveIgnorarEventosDeServidorEnviadosPeloCliente() throws Exception {
        enviar(sessaoAria, "{\"type\":\"STATUS\",\"content\":\"x\"}");
        enviar(sessaoAria, "{\"type\":\"ITENS\",\"content\":\"x\"}");
        enviar(sessaoAria, "{\"type\":\"NOTA\",\"content\":\"x\"}");

        assertEquals(3, recebidas(sessaoAria, "SISTEMA").size());
        assertTrue(recebidas(sessaoBrom, "STATUS").isEmpty());
        assertTrue(recebidas(sessaoBrom, "ITENS").isEmpty());
        assertTrue(recebidas(sessaoMestre, "NOTA").isEmpty());
    }
}
