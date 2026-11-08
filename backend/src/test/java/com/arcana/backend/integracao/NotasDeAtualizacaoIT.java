package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotasDeAtualizacaoIT extends IntegracaoTest {

    private static final String URL = "/api/notas-de-atualizacao/vista";

    @Test
    void deveGuardarPorContaAUltimaVersaoDasNotasVistas() throws Exception {
        Conta jogador = registrar("JOGADOR");
        Conta mestre = registrar("MESTRE");

        getComo(jogador.token(), URL).andExpect(status().isOk()).andExpect(jsonPath("$.versao").isEmpty());
        putComo(jogador.token(), URL, Map.of("versao", "1.1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value("1.1.0"));

        getComo(jogador.token(), URL).andExpect(jsonPath("$.versao").value("1.1.0"));
        getComo(mestre.token(), URL).andExpect(jsonPath("$.versao").isEmpty());
    }

    @Test
    void deveRecusarVersaoInvalidaEAcessoSemLogin() throws Exception {
        Conta jogador = registrar("JOGADOR");

        putComo(jogador.token(), URL, Map.of("versao", "nova")).andExpect(status().isBadRequest());
        getComo(null, URL).andExpect(status().isUnauthorized());
        putComo(null, URL, Map.of("versao", "1.1.0")).andExpect(status().isUnauthorized());
    }
}
