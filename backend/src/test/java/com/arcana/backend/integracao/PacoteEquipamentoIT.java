package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PacoteEquipamentoIT extends IntegracaoTest {

    private static final Map<String, Object> EXPLORADOR = Map.of("pacote", "EXPLORADOR");
    private static final Map<String, Object> ESTUDIOSO = Map.of("pacote", "ESTUDIOSO");

    private String urlDoPacote(Long fichaId) {
        return "/api/characters/" + fichaId + "/pacote-equipamento";
    }

    @Test
    void deveListarOsSetePacotesDoCatalogo() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(jogador.token(), "/api/pacotes-equipamento")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].nome").value("Assaltante"))
                .andExpect(jsonPath("$[4].itens", hasItem("Tocha (10)")));
    }

    @Test
    void devePermitirAplicarETrocarPacoteEmFichaLivre() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");

        putComo(dono.token(), urlDoPacote(fichaId), EXPLORADOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacoteEquipamento").value("EXPLORADOR"))
                .andExpect(jsonPath("$.items", hasItem("Saco de dormir")));
        putComo(dono.token(), urlDoPacote(fichaId), ESTUDIOSO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacoteEquipamento").value("ESTUDIOSO"))
                .andExpect(jsonPath("$.items", hasSize(7)));
        putComo(registrar("JOGADOR").token(), urlDoPacote(fichaId), EXPLORADOR).andExpect(status().isForbidden());
    }

    @Test
    void deveBloquearPacoteEnquantoAFichaTemSolicitacaoPendente() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long fichaId = criarPersonagem(jogador, "PLAYER");
        solicitarEntrada(jogador, mesaId, fichaId);

        putComo(jogador.token(), urlDoPacote(fichaId), EXPLORADOR).andExpect(status().isConflict());
    }

    @Test
    void deveBloquearPacoteDoParticipanteELiberarAoSairDaMesa() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        String url = urlDoPacote(jogador.personagemId());

        putComo(jogador.conta().token(), url, EXPLORADOR).andExpect(status().isConflict());

        postComo(jogador.conta().token(), "/api/tables/" + mesaId + "/sair", null).andExpect(status().isOk());
        putComo(jogador.conta().token(), url, EXPLORADOR).andExpect(status().isOk());
    }
}
