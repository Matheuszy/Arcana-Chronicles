package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FichaPorAbasIT extends IntegracaoTest {

    private static final List<String> ABAS = List.of("personagem", "atributos", "habilidades", "equipamento", "magias",
            "rolagens");

    @Test
    void deveDevolverCadaAbaSoComOsCamposDela() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");
        String base = "/api/characters/" + id + "/ficha/";

        getComo(dono.token(), base + "magias")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells").isArray())
                .andExpect(jsonPath("$.items").doesNotExist())
                .andExpect(jsonPath("$.name").doesNotExist());
        getComo(dono.token(), base + "equipamento")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moedasOuro").value(0))
                .andExpect(jsonPath("$.spells").doesNotExist());
        getComo(dono.token(), base + "personagem")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes[0].classe").value("Guerreiro"))
                .andExpect(jsonPath("$.skills").doesNotExist());
        getComo(dono.token(), base + "atributos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.destreza").value(12))
                .andExpect(jsonPath("$.classeArmaduraTotal").value(12))
                .andExpect(jsonPath("$.idiomas").doesNotExist());
        getComo(dono.token(), base + "habilidades")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idiomas").isArray());
        getComo(dono.token(), base + "rolagens")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes[0].dadoVida").value(8))
                .andExpect(jsonPath("$.moedasRoladas").value(false));
    }

    @Test
    void deveSalvarUmaAbaSemApagarAsOutras() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");
        String base = "/api/characters/" + id + "/ficha/";

        putComo(dono.token(), base + "magias", Map.of(
                "spells", List.of(Map.of("name", "Mísseis Mágicos", "level", 1, "description", "3 dardos")),
                "spellSlots", Map.of("1", 2),
                "spellSlotsCurrent", Map.of("1", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells", hasSize(1)))
                .andExpect(jsonPath("$.spellSlotsCurrent.1").value(1));

        putComo(dono.token(), base + "equipamento", Map.of(
                "items", List.of("Tocha", "Corda"),
                "equipamentos", List.of(Map.of("tipo", "ARMA", "nome", "Espada longa", "dano", "1d8")),
                "moedasOuro", 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.moedasOuro").value(12));

        putComo(dono.token(), base + "habilidades", Map.of("idiomas", List.of("Comum", "Élfico"), "talentos", "Alerta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idiomas[1]").value("Élfico"));

        getComo(dono.token(), "/api/characters/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells[0].name").value("Mísseis Mágicos"))
                .andExpect(jsonPath("$.items[0]").value("Tocha"))
                .andExpect(jsonPath("$.equipamentos[0].nome").value("Espada longa"))
                .andExpect(jsonPath("$.idiomas", hasSize(2)))
                .andExpect(jsonPath("$.classes[0].classe").value("Guerreiro"))
                .andExpect(jsonPath("$.attributes.destreza").value(12));
    }

    @Test
    void deveValidarCadaAbaAoSalvar() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");
        String base = "/api/characters/" + id + "/ficha/";

        putComo(dono.token(), base + "personagem", Map.of("name", "", "kind", "PLAYER", "classes", List.of()))
                .andExpect(status().isBadRequest());
        putComo(dono.token(), base + "equipamento", Map.of("moedasOuro", -1))
                .andExpect(status().isBadRequest());
        putComo(dono.token(), base + "personagem", Map.of(
                "name", "Thorn", "kind", "MONSTER", "raca", "Anão", "alinhamento", "Neutro",
                "classes", List.of(Map.of("classe", "Guerreiro", "nivel", 2))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarAsAbasDaFichaParaQuemNaoEDono() throws Exception {
        Conta dono = registrar("JOGADOR");
        Conta outro = registrar("JOGADOR");
        Long id = criarPersonagem(dono, "PLAYER");

        for (String aba : ABAS) {
            getComo(outro.token(), "/api/characters/" + id + "/ficha/" + aba).andExpect(status().isForbidden());
            getComo(null, "/api/characters/" + id + "/ficha/" + aba).andExpect(status().isUnauthorized());
        }
        putComo(outro.token(), "/api/characters/" + id + "/ficha/habilidades", Map.of("talentos", "Roubado"))
                .andExpect(status().isForbidden());
    }
}
