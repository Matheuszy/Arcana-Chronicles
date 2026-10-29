package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersonagemIT extends IntegracaoTest {

    @Test
    void devePermitirAoDonoCriarConsultarEditarERemoverFicha() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");

        getComo(dono.token(), "/api/characters/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("PLAYER"));

        Map<String, Object> editada = new HashMap<>(fichaDoTipo("PLAYER"));
        editada.put("name", "Thorn Reforjado");
        editada.put("level", 5);
        putComo(dono.token(), "/api/characters/" + id, editada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thorn Reforjado"))
                .andExpect(jsonPath("$.level").value(5));

        deleteComo(dono.token(), "/api/characters/" + id).andExpect(status().isNoContent());
        getComo(dono.token(), "/api/characters/" + id).andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirOutroUsuarioDeVerEditarOuRemoverAFicha() throws Exception {
        Conta dono = registrar("JOGADOR");
        Conta intruso = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");

        getComo(intruso.token(), "/api/characters/" + id).andExpect(status().isForbidden());
        putComo(intruso.token(), "/api/characters/" + id, fichaDoTipo("PLAYER")).andExpect(status().isForbidden());
        deleteComo(intruso.token(), "/api/characters/" + id).andExpect(status().isForbidden());

        getComo(dono.token(), "/api/characters/" + id).andExpect(status().isOk());
    }

    @Test
    void deveListarApenasAsFichasDoUsuarioComFiltroPorTipo() throws Exception {
        Conta mestre = registrar("MESTRE");
        Conta outro = registrar("MESTRE");
        criarPersonagem(mestre, "PLAYER");
        criarPersonagem(mestre, "BOSS");
        criarPersonagem(mestre, "BOSS");
        criarPersonagem(outro, "BOSS");

        getComo(mestre.token(), "/api/characters")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        getComo(mestre.token(), "/api/characters?kind=BOSS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].kind", everyItem(is("BOSS"))));
    }

    @Test
    void deveRecusarFichaComAtributoForaDoLimite() throws Exception {
        Conta dono = registrar("JOGADOR");
        Map<String, Object> ficha = new HashMap<>(fichaDoTipo("PLAYER"));
        ficha.put("attributes", Map.of(
                "forca", 25, "destreza", 10, "constituicao", 10,
                "inteligencia", 10, "sabedoria", 10, "carisma", 10));

        postComo(dono.token(), "/api/characters", ficha)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."));
    }

    @Test
    void deveRecusarTipoDeFichaInexistente() throws Exception {
        Conta dono = registrar("JOGADOR");
        Map<String, Object> ficha = new HashMap<>(fichaDoTipo("PLAYER"));
        ficha.put("kind", "DRAGAO");

        postComo(dono.token(), "/api/characters", ficha).andExpect(status().isBadRequest());
    }
}
