package com.arcana.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatIT extends IntegracaoTest {

    private final List<ClienteChat> clientes = new ArrayList<>();

    @AfterEach
    void fecharClientes() {
        clientes.forEach(ClienteChat::fechar);
    }

    private ClienteChat conectar(Long mesaId, Conta conta) throws Exception {
        ClienteChat cliente = conectarAoChat(mesaId, conta.token());
        clientes.add(cliente);
        cliente.aguardar("HISTORICO");
        return cliente;
    }

    private static final Map<String, Object> ROLAGEM_D20 = Map.of(
            "type", "ROLAGEM",
            "canal", "ROLAGENS",
            "rollRequest", Map.of("lados", 20, "quantidade", 1, "modificador", 3, "modo", "NORMAL"));

    @Test
    void deveRecusarConexaoSemTokenOuComTokenInvalido() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        assertEquals(401, statusDoHandshake(mesaId, null));
        assertEquals(401, statusDoHandshake(mesaId, "token.invalido.qualquer"));
    }

    @Test
    void deveRecusarConexaoDeQuemNaoParticipaDaMesa() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta estranho = registrar("JOGADOR");

        assertEquals(403, statusDoHandshake(mesaId, estranho.token()));
        assertEquals(404, statusDoHandshake(999_999L, mestre.token()));
    }

    @Test
    void deveRecusarConexaoDeOrigemNaoPermitida() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);

        assertEquals(403, statusDoHandshake(mesaId, mestre.token(), "http://site-malicioso.example"));
        assertEquals(101, statusDoHandshake(mesaId, mestre.token()));
    }

    @Test
    void deveEntregarHistoricoEPresencaParaQuemConecta() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);

        ClienteChat chatDoMestre = conectar(mesaId, mestre);
        chatDoMestre.enviar(Map.of("type", "TEXTO", "canal", "CONVERSA", "content", "Bem-vindos à taverna."));
        chatDoMestre.aguardarConteudo("Bem-vindos à taverna.");

        ClienteChat chatDoJogador = conectarAoChat(mesaId, jogador.conta().token());
        clientes.add(chatDoJogador);
        JsonNode sincronizacao = chatDoJogador.aguardar("HISTORICO");

        List<String> conversa = new ArrayList<>();
        sincronizacao.get("historico").get("CONVERSA").forEach(m -> conversa.add(m.get("content").asText()));
        assertTrue(conversa.contains("Bem-vindos à taverna."));
        assertEquals(2, sincronizacao.get("presentes").size());
        assertEquals("MESTRE", sincronizacao.get("presentes").get(0).get("role").asText());

        assertEquals(2, chatDoMestre.aguardar("PRESENCA").get("presentes").size());
    }

    @Test
    void deveCalcularRolagemNoServidorEUsarAutorDaSessao() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        ClienteChat chatDoMestre = conectar(mesaId, mestre);
        ClienteChat chatDoJogador = conectar(mesaId, jogador.conta());

        chatDoJogador.enviar(Map.of(
                "type", "ROLAGEM",
                "canal", "ROLAGENS",
                "authorName", "Mestre Falso",
                "diceResult", Map.of("expression", "1d20", "rolls", List.of(20), "modifier", 0, "total", 999),
                "rollRequest", Map.of("lados", 20, "quantidade", 1, "modificador", 3, "modo", "NORMAL")));

        JsonNode rolagem = chatDoMestre.aguardar("ROLAGEM");
        int dado = rolagem.get("diceResult").get("rolls").get(0).asInt();
        assertTrue(dado >= 1 && dado <= 20);
        assertEquals(dado + 3, rolagem.get("diceResult").get("total").asInt());
        assertEquals(jogador.conta().username(), rolagem.get("authorName").asText());
        assertEquals(String.valueOf(jogador.conta().id()), rolagem.get("authorId").asText());
        assertEquals("ROLAGENS", rolagem.get("canal").asText());
    }

    @Test
    void deveBloquearMensagensEnquantoAMesaEstaPausada() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        ClienteChat chatDoMestre = conectar(mesaId, mestre);
        ClienteChat chatDoJogador = conectar(mesaId, jogador.conta());

        patchComo(mestre.token(), "/api/tables/" + mesaId + "/status", Map.of("status", "PAUSADA"))
                .andExpect(status().isOk());
        assertEquals("PAUSADA", chatDoJogador.aguardar("STATUS_MESA").get("content").asText());

        chatDoJogador.enviar(ROLAGEM_D20);
        assertTrue(chatDoJogador.aguardar("SISTEMA").get("content").asText().startsWith("A mesa está pausada."));

        patchComo(mestre.token(), "/api/tables/" + mesaId + "/status", Map.of("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk());
        chatDoJogador.aguardar("STATUS_MESA");
        chatDoJogador.enviar(ROLAGEM_D20);
        assertEquals(jogador.conta().username(), chatDoMestre.aguardar("ROLAGEM").get("authorName").asText());
    }

    @Test
    void deveEncerrarSessaoEApagarHistoricoQuandoOMestreEncerraAMesa() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        conectar(mesaId, mestre);
        ClienteChat chatDoJogador = conectar(mesaId, jogador.conta());

        deleteComo(mestre.token(), "/api/tables/" + mesaId).andExpect(status().isConflict());

        patchComo(mestre.token(), "/api/tables/" + mesaId + "/status", Map.of("status", "ENCERRADA"))
                .andExpect(status().isOk());

        assertEquals("O mestre encerrou a mesa.", chatDoJogador.aguardar("SESSAO_ENCERRADA").get("content").asText());
        assertEquals(WebSocket.NORMAL_CLOSURE, chatDoJogador.aguardarFechamento());
        assertEquals(403, statusDoHandshake(mesaId, jogador.conta().token()));

        deleteComo(mestre.token(), "/api/tables/" + mesaId).andExpect(status().isNoContent());
    }

    @Test
    void deveDesconectarJogadorRemovidoEImpedirQueVolte() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        ClienteChat chatDoMestre = conectar(mesaId, mestre);
        ClienteChat chatDoJogador = conectar(mesaId, jogador.conta());

        deleteComo(mestre.token(), "/api/tables/" + mesaId + "/participantes/" + jogador.conta().id())
                .andExpect(status().isOk());

        assertEquals("O mestre removeu você da mesa.", chatDoJogador.aguardar("REMOVIDO_DA_MESA").get("content").asText());
        assertEquals(WebSocket.NORMAL_CLOSURE, chatDoJogador.aguardarFechamento());
        chatDoMestre.aguardarConteudo("O mestre removeu " + jogador.conta().username() + " da mesa.");
        chatDoMestre.aguardar("PARTICIPANTES");
        assertEquals(403, statusDoHandshake(mesaId, jogador.conta().token()));
    }

    @Test
    void deveAvisarAMesaQuandoUmNovoJogadorEAceito() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        ClienteChat chatDoMestre = conectar(mesaId, mestre);

        jogadorAceitoNaMesa(mestre, mesaId);

        chatDoMestre.aguardar("PARTICIPANTES");
    }
}
