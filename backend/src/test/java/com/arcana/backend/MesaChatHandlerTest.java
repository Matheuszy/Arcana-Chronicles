package com.arcana.backend;

import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesaChatHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MesaChatHandler handler;
    private WebSocketSession remetente;
    private WebSocketSession outroJogador;

    @BeforeEach
    void setUp() throws Exception {
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
        handler = new MesaChatHandler(objectMapper, new RolagemDadosService(sempreQuatro));

        remetente = sessaoNaMesa("a");
        outroJogador = sessaoNaMesa("b");
        handler.afterConnectionEstablished(remetente);
        handler.afterConnectionEstablished(outroJogador);
        clearInvocations(remetente, outroJogador);
    }

    private WebSocketSession sessaoNaMesa(String id) {
        WebSocketSession sessao = mock(WebSocketSession.class);
        when(sessao.getId()).thenReturn(id);
        when(sessao.getUri()).thenReturn(URI.create("ws://localhost/ws/mesas/1"));
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
    void deveSubstituirResultadoForjadoPeloCalculoDoServidor() throws Exception {
        String payload = """
                {"tableId":"1","authorId":"x","authorName":"Trapaceiro","type":"ROLAGEM",
                 "content":"rolou","createdAt":"2026-10-29T00:00:00Z",
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
                {"tableId":"1","authorId":"x","authorName":"Jogador","type":"ROLAGEM",
                 "content":"","createdAt":"2026-10-29T00:00:00Z",
                 "rollRequest":{"lados":7,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
                """;

        handler.handleMessage(remetente, new TextMessage(payload));

        List<JsonNode> doRemetente = mensagensRecebidas(remetente);
        assertEquals(1, doRemetente.size());
        assertEquals("SISTEMA", doRemetente.get(0).get("type").asText());
        assertTrue(mensagensRecebidas(outroJogador).isEmpty());
    }
}
