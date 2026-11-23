package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MagiaCatalogoIT extends IntegracaoTest {

    private static final String URL = "/api/catalogo-magias";

    @Test
    void deveTerCarregadoTodasAsMagiasETruquesDoSrd() {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalogo_magias", Integer.class);
        Integer truques = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalogo_magias WHERE nivel = 0",
                Integer.class);

        assertEquals(319, total);
        assertEquals(24, truques);
    }

    @Test
    void deveSugerirMagiasPorParteDoNomePriorizandoAClasseDaFicha() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(jogador.token(), URL + "?nome=fire&classes=Clérigo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Fire Storm"))
                .andExpect(jsonPath("$[0].classes", contains("Cleric", "Druid", "Sorcerer")));
        getComo(jogador.token(), URL + "?nome=fire&nivel=0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Fire Bolt"));
        getComo(jogador.token(), URL + "?nome=zzz").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveDetalharMagiaComCadaCampoSeparado() throws Exception {
        Conta mestre = registrar("MESTRE");

        getComo(mestre.token(), URL + "/fireball")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fireball"))
                .andExpect(jsonPath("$.nivel").value(3))
                .andExpect(jsonPath("$.escola").value("Evocation"))
                .andExpect(jsonPath("$.tempoConjuracao").value("1 action"))
                .andExpect(jsonPath("$.alcance").value("150 feet"))
                .andExpect(jsonPath("$.componentes").value("V, S, M"))
                .andExpect(jsonPath("$.material").value("A tiny ball of bat guano and sulfur."))
                .andExpect(jsonPath("$.duracao").value("Instantaneous"))
                .andExpect(jsonPath("$.concentracao").value(false))
                .andExpect(jsonPath("$.ritual").value(false))
                .andExpect(jsonPath("$.versaoRegras").value("SRD 5.1"));
        getComo(mestre.token(), URL + "/detect-magic")
                .andExpect(jsonPath("$.concentracao").value(true))
                .andExpect(jsonPath("$.ritual").value(true));
        getComo(mestre.token(), URL + "/magia-inexistente").andExpect(status().isNotFound());
    }

    private Map<String, Object> copiaDaBolaDeFogo(String descricao) {
        Map<String, Object> magia = new HashMap<>();
        magia.put("name", "Fireball");
        magia.put("level", 3);
        magia.put("description", descricao);
        magia.put("school", "Evocation");
        magia.put("castingTime", "1 action");
        magia.put("range", "150 feet");
        magia.put("components", "V, S, M");
        magia.put("material", "A tiny ball of bat guano and sulfur.");
        magia.put("duration", "Instantaneous");
        magia.put("concentration", false);
        magia.put("ritual", false);
        magia.put("higherLevels", campoDoCatalogo("niveis_superiores"));
        magia.put("classes", "Sorcerer, Wizard");
        magia.put("catalogKey", "fireball");
        return magia;
    }

    private String campoDoCatalogo(String coluna) {
        return jdbcTemplate.queryForObject(
                "SELECT " + coluna + " FROM catalogo_magias WHERE chave = ?", String.class, "fireball");
    }

    private Map<String, Object> abaDeMagias(Map<String, Object> magia) {
        return Map.of("spells", List.of(magia), "spellSlots", Map.of("3", 2), "spellSlotsCurrent", Map.of("3", 2));
    }

    @Test
    void deveCopiarMagiaDoCatalogoParaAFichaEMarcarQuandoForEditada() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        String url = "/api/characters/" + fichaId + "/ficha/magias";
        String descricaoOriginal = campoDoCatalogo("descricao");

        putComo(dono.token(), url, abaDeMagias(copiaDaBolaDeFogo(descricaoOriginal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells[0].catalogKey").value("fireball"))
                .andExpect(jsonPath("$.spells[0].range").value("150 feet"))
                .andExpect(jsonPath("$.spells[0].editada").value(false));

        putComo(dono.token(), url, abaDeMagias(copiaDaBolaDeFogo("Minha bola de fogo.")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spells[0].description").value("Minha bola de fogo."))
                .andExpect(jsonPath("$.spells[0].editada").value(true));
        getComo(dono.token(), URL + "/fireball").andExpect(jsonPath("$.descricao").value(descricaoOriginal));

        putComo(registrar("JOGADOR").token(), url, abaDeMagias(copiaDaBolaDeFogo("Invasão")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarAMesmaMagiaDoCatalogoDuasVezesNaFicha() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");

        putComo(dono.token(), "/api/characters/" + fichaId + "/ficha/magias", Map.of(
                "spells", List.of(copiaDaBolaDeFogo(null), copiaDaBolaDeFogo("de novo")),
                "spellSlots", Map.of("3", 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveNegarAcessoSemLoginENaoPermitirAlterarOCatalogo() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(null, URL + "?nome=fire").andExpect(status().isUnauthorized());
        getComo(null, URL + "/fireball").andExpect(status().isUnauthorized());
        postComo(jogador.token(), URL, Map.of("nome", "Nova")).andExpect(status().isMethodNotAllowed());
        putComo(jogador.token(), URL + "/fireball", Map.of("nome", "Bola")).andExpect(status().isMethodNotAllowed());
        deleteComo(jogador.token(), URL + "/fireball").andExpect(status().isMethodNotAllowed());
    }
}
