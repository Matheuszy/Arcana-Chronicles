package com.arcana.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolagemDeFichaIT extends IntegracaoTest {

    private final List<ClienteChat> clientes = new ArrayList<>();

    @AfterEach
    void fecharClientes() {
        clientes.forEach(ClienteChat::fechar);
    }

    @Test
    void devePermitirApenasAoDonoRolarVidaEConsultarOHistorico() throws Exception {
        Conta dono = registrar("JOGADOR");
        Conta outro = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        String url = "/api/characters/" + fichaId + "/rolagens";

        JsonNode resultado = corpo(postComo(dono.token(), url + "/vida", Map.of("lados", 8, "quantidade", 1, "modificador", 0))
                .andExpect(status().isOk()));
        int total = resultado.get("rolagem").get("total").asInt();
        assertTrue(total >= 1 && total <= 8);
        assertEquals(10 + total, resultado.get("ficha").get("hpMax").asInt());

        postComo(outro.token(), url + "/vida", Map.of("lados", 8, "quantidade", 1)).andExpect(status().isForbidden());
        getComo(outro.token(), url).andExpect(status().isForbidden());
        getComo(dono.token(), url)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("VIDA"))
                .andExpect(jsonPath("$[0].total").value(total));
    }

    @Test
    void deveGastarDadosDeVidaDisponiveisDaClasse() throws Exception {
        Conta dono = registrar("JOGADOR");
        Map<String, Object> ficha = new HashMap<>(fichaDoTipo("PLAYER"));
        ficha.put("hpCurrent", 1);
        ficha.put("hpMax", 30);
        ficha.put("classes", List.of(Map.of("classe", "Guerreiro", "nivel", 2, "dadoVida", 10, "dadosVidaDisponiveis", 2)));
        Long fichaId = corpo(postComo(dono.token(), "/api/characters", ficha).andExpect(status().isCreated()))
                .get("id").asLong();
        String url = "/api/characters/" + fichaId + "/rolagens/dados-de-vida";

        postComo(dono.token(), url, Map.of("classe", 0, "quantidade", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ficha.classes[0].dadosVidaDisponiveis").value(0))
                .andExpect(jsonPath("$.rolagem.tipo").value("DADOS_DE_VIDA"));
        postComo(dono.token(), url, Map.of("classe", 0, "quantidade", 1)).andExpect(status().isConflict());
    }

    @Test
    void deveRolarMoedasApenasNaFichaDoDono() throws Exception {
        Conta dono = registrar("JOGADOR");
        Conta outro = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        String url = "/api/characters/" + fichaId + "/rolagens/moedas";
        Map<String, Object> pedido = Map.of("moeda", "PRATA", "lados", 6, "quantidade", 2, "multiplicador", 10);

        JsonNode resultado = corpo(postComo(dono.token(), url, pedido).andExpect(status().isOk()));
        int total = resultado.get("rolagem").get("total").asInt();
        assertTrue(total >= 20 && total <= 120 && total % 10 == 0);
        assertEquals(total, resultado.get("ficha").get("moedasPrata").asInt());

        postComo(outro.token(), url, pedido).andExpect(status().isForbidden());
        postComo(dono.token(), url, Map.of("moeda", "OURO", "lados", 7, "quantidade", 1)).andExpect(status().isBadRequest());
    }

    @Test
    void deveLiberarOHistoricoDaFichaDeParticipanteApenasAoMestre() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        JogadorNaMesa colega = jogadorAceitoNaMesa(mestre, mesaId);
        postComo(jogador.conta().token(), "/api/characters/" + jogador.personagemId() + "/rolagens/vida",
                Map.of("lados", 6, "quantidade", 1)).andExpect(status().isOk());
        String url = "/api/tables/" + mesaId + "/fichas/" + jogador.personagemId() + "/rolagens";

        getComo(mestre.token(), url).andExpect(status().isOk()).andExpect(jsonPath("$[0].tipo").value("VIDA"));
        getComo(colega.conta().token(), url).andExpect(status().isForbidden());
    }

    @Test
    void devePublicarRolagemDaFichaNoChatDaMesaComSessaoAtiva() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        ClienteChat chatDoMestre = conectarAoChat(mesaId, mestre.token());
        clientes.add(chatDoMestre);
        chatDoMestre.aguardar("HISTORICO");

        postComo(jogador.conta().token(), "/api/characters/" + jogador.personagemId() + "/rolagens/vida",
                Map.of("lados", 12, "quantidade", 1, "modificador", 2)).andExpect(status().isOk());

        JsonNode rolagem = chatDoMestre.aguardar("ROLAGEM");
        assertEquals("ROLAGENS", rolagem.get("canal").asText());
        assertEquals(jogador.conta().username(), rolagem.get("authorName").asText());
        assertEquals("1d12+2", rolagem.get("diceResult").get("expression").asText());
        assertTrue(rolagem.get("diceResult").get("label").asText().contains("Vida máxima +"));
    }
}
