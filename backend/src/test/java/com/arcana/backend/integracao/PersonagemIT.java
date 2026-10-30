package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
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
    void devePersistirTruquesMagiasEEspacosDeMagiaRestantes() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");

        Map<String, Object> ficha = new HashMap<>(fichaDoTipo("PLAYER"));
        ficha.put("spells", List.of(
                Map.of("name", "Raio de Fogo", "level", 0),
                Map.of("name", "Bola de Fogo", "level", 3)));
        ficha.put("spellSlots", Map.of("3", 3));
        ficha.put("spellSlotsCurrent", Map.of("3", 1));
        ficha.put("habilidades", "Surto de Ação: uma ação extra por descanso");
        putComo(dono.token(), "/api/characters/" + id, ficha).andExpect(status().isOk());

        getComo(dono.token(), "/api/characters/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells", hasSize(2)))
                .andExpect(jsonPath("$.spellSlots.3").value(3))
                .andExpect(jsonPath("$.spellSlotsCurrent.3").value(1))
                .andExpect(jsonPath("$.habilidades").value("Surto de Ação: uma ação extra por descanso"));

        ficha.put("spellSlots", Map.of("1", 2));
        putComo(dono.token(), "/api/characters/" + id, ficha).andExpect(status().isBadRequest());
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
        criarPersonagem(mestre, "MONSTER");
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

    @Test
    void deveLimitarOsTiposDeFichaAoPapelDaConta() throws Exception {
        Conta jogador = registrar("JOGADOR");
        Conta mestre = registrar("MESTRE");

        postComo(jogador.token(), "/api/characters", fichaDoTipo("MONSTER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Contas de jogador só criam personagens do tipo jogador."));
        postComo(mestre.token(), "/api/characters", fichaDoTipo("PLAYER"))
                .andExpect(status().isForbidden());
        postComo(mestre.token(), "/api/characters", fichaDoTipo("NPC_FRIENDLY")).andExpect(status().isCreated());

        Long fichaDoJogador = criarPersonagem(jogador, "PLAYER");
        putComo(jogador.token(), "/api/characters/" + fichaDoJogador, fichaDoTipo("BOSS"))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirEditarFichaAntigaForaDaRegraSemTrocarOTipo() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long fichaAntiga = criarFichaAntigaForaDaRegra(mestre, true, "PLAYER");

        Map<String, Object> editada = new HashMap<>(fichaDoTipo("PLAYER"));
        editada.put("name", "Herói Antigo");
        putComo(mestre.token(), "/api/characters/" + fichaAntiga, editada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("PLAYER"));
    }

    @Test
    void deveGravarClasseSubclasseItensEProficiencias() throws Exception {
        Conta dono = registrar("JOGADOR");
        Map<String, Object> ficha = new HashMap<>(fichaDoTipo("PLAYER"));
        ficha.put("classe", "Ladino");
        ficha.put("subclasse", "Assassino");
        ficha.put("proficiencyBonus", 3);
        ficha.put("savingThrows", Map.of("destreza", 3, "inteligencia", 3));
        ficha.put("skills", Map.of("furtividade", 6, "persuasao", 3));
        ficha.put("items", List.of("Ferramentas de ladrão", "Corda de 15 m"));

        Long id = corpo(postComo(dono.token(), "/api/characters", ficha).andExpect(status().isCreated()))
                .get("id").asLong();

        getComo(dono.token(), "/api/characters/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classe").value("Ladino"))
                .andExpect(jsonPath("$.subclasse").value("Assassino"))
                .andExpect(jsonPath("$.proficiencyBonus").value(3))
                .andExpect(jsonPath("$.savingThrows.destreza").value(3))
                .andExpect(jsonPath("$.savingThrows.inteligencia").value(3))
                .andExpect(jsonPath("$.savingThrows.forca").doesNotExist())
                .andExpect(jsonPath("$.skills.furtividade").value(6))
                .andExpect(jsonPath("$.skills.persuasao").value(3))
                .andExpect(jsonPath("$.items", contains("Ferramentas de ladrão", "Corda de 15 m")));
    }

    @Test
    void deveRecusarPericiaDesconhecidaEItensForaDoLimite() throws Exception {
        Conta dono = registrar("JOGADOR");

        Map<String, Object> comPericiaInvalida = new HashMap<>(fichaDoTipo("PLAYER"));
        comPericiaInvalida.put("skills", Map.of("voar", 2));
        postComo(dono.token(), "/api/characters", comPericiaInvalida)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Perícia desconhecida: voar"));

        Map<String, Object> comBonusForaDoLimite = new HashMap<>(fichaDoTipo("PLAYER"));
        comBonusForaDoLimite.put("skills", Map.of("furtividade", 21));
        postComo(dono.token(), "/api/characters", comBonusForaDoLimite).andExpect(status().isBadRequest());

        Map<String, Object> comItemLongo = new HashMap<>(fichaDoTipo("PLAYER"));
        comItemLongo.put("items", List.of("x".repeat(101)));
        postComo(dono.token(), "/api/characters", comItemLongo).andExpect(status().isBadRequest());

        Map<String, Object> comItensDemais = new HashMap<>(fichaDoTipo("PLAYER"));
        comItensDemais.put("items", Collections.nCopies(51, "Tocha"));
        postComo(dono.token(), "/api/characters", comItensDemais).andExpect(status().isBadRequest());
    }
}
