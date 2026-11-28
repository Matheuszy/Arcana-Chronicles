package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipamentoCatalogoIT extends IntegracaoTest {

    private static final String URL = "/api/catalogo-equipamentos";

    private int contar(String categoria) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM catalogo_equipamentos WHERE categoria = ?",
                Integer.class, categoria);
    }

    @Test
    void deveTerCarregadoArmasArmadurasEquipamentoDeAventuraEFerramentasDoSrd() {
        assertEquals(37, contar("ARMA"));
        assertEquals(13, contar("ARMADURA"));
        assertEquals(99, contar("EQUIPAMENTO_DE_AVENTURA"));
        assertEquals(35, contar("FERRAMENTA"));
    }

    @Test
    void deveTerOsItensDosPacotesDeEquipamentoComOMesmoNomeNoCatalogo() {
        Integer semCorrespondencia = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT i.item) FROM pacote_equipamento_itens i
                WHERE i.item IN ('Mochila', 'Pé de cabra', 'Martelo', 'Caixa de isqueiro', 'Odre',
                                 'Corda de cânhamo (15 m)', 'Saco de dormir', 'Kit de refeição', 'Lanterna furta-fogo')
                  AND NOT EXISTS (SELECT 1 FROM catalogo_equipamentos c WHERE c.nome = i.item)
                """, Integer.class);

        assertEquals(0, semCorrespondencia);
    }

    @Test
    void deveSugerirPorParteDoNomeSemAcentoEFiltrarPorCategoria() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(jogador.token(), URL + "?nome=maca")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maça"))
                .andExpect(jsonPath("$[1].nome").value("Maça estrela"));
        getComo(jogador.token(), URL + "?nome=espada&categoria=ARMA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].categoria", everyItem(is("ARMA"))));
        getComo(jogador.token(), URL + "?nome=cajado&categoria=EQUIPAMENTO_DE_AVENTURA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        getComo(jogador.token(), URL + "?nome=zzz").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getComo(jogador.token(), URL + "?nome=espada&categoria=INVALIDA").andExpect(status().isBadRequest());
    }

    @Test
    void deveDetalharArmaComDanoPropriedadesPesoEmQuilosEPreco() throws Exception {
        Conta mestre = registrar("MESTRE");

        getComo(mestre.token(), URL + "/longsword")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Espada longa"))
                .andExpect(jsonPath("$.categoria").value("ARMA"))
                .andExpect(jsonPath("$.subcategoria").value("Arma marcial corpo a corpo"))
                .andExpect(jsonPath("$.dano").value("1d8"))
                .andExpect(jsonPath("$.tipoDano").value("Cortante"))
                .andExpect(jsonPath("$.propriedades").value("Versátil (1d10)"))
                .andExpect(jsonPath("$.pesoKg").value(1.5))
                .andExpect(jsonPath("$.precoEmPc").value(1500))
                .andExpect(jsonPath("$.preco").value("15 po"))
                .andExpect(jsonPath("$.versaoRegras").value("SRD 5.1"));
        getComo(mestre.token(), URL + "/arrow")
                .andExpect(jsonPath("$.nome").value("Flechas"))
                .andExpect(jsonPath("$.quantidade").value(20))
                .andExpect(jsonPath("$.pesoKg").value(0.5));
        getComo(mestre.token(), URL + "/equipamento-inexistente").andExpect(status().isNotFound());
    }

    @Test
    void deveDetalharArmaduraComRegraDeClasseDeArmaduraForcaMinimaEFurtividade() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(jogador.token(), URL + "/chain-mail")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cota de malha"))
                .andExpect(jsonPath("$.classeArmadura").value(16))
                .andExpect(jsonPath("$.regraDeClasseArmadura").value("16"))
                .andExpect(jsonPath("$.forcaMinima").value(13))
                .andExpect(jsonPath("$.desvantagemFurtividade").value(true))
                .andExpect(jsonPath("$.pesoKg").value(27.5));
        getComo(jogador.token(), URL + "/breastplate")
                .andExpect(jsonPath("$.regraDeClasseArmadura").value("14 + Des (máx. 2)"))
                .andExpect(jsonPath("$.desvantagemFurtividade").value(false));
        getComo(jogador.token(), URL + "/shield").andExpect(jsonPath("$.regraDeClasseArmadura").value("+2"));
    }

    private Map<String, Object> espadaLongaDoCatalogo(String nome, double pesoKg) {
        Map<String, Object> arma = new HashMap<>();
        arma.put("tipo", "ARMA");
        arma.put("nome", nome);
        arma.put("dano", "1d8");
        arma.put("atributo", "forca");
        arma.put("propriedades", "Versátil (1d10)");
        arma.put("tipoDano", "Cortante");
        arma.put("pesoKg", pesoKg);
        arma.put("catalogKey", "longsword");
        return arma;
    }

    private Map<String, Object> abaDeEquipamento(List<Map<String, Object>> equipamentos) {
        return Map.of("equipamentos", equipamentos, "items", List.of());
    }

    @Test
    void deveCopiarArmaDoCatalogoParaAFichaComPesoEMarcarQuandoForEditada() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        String url = "/api/characters/" + fichaId + "/ficha/equipamento";

        putComo(dono.token(), url, abaDeEquipamento(List.of(
                espadaLongaDoCatalogo("Espada longa", 1.5), espadaLongaDoCatalogo("Espada do avô", 1.5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipamentos[0].catalogKey").value("longsword"))
                .andExpect(jsonPath("$.equipamentos[0].tipoDano").value("Cortante"))
                .andExpect(jsonPath("$.equipamentos[0].pesoKg").value(1.5))
                .andExpect(jsonPath("$.equipamentos[0].editada").value(false))
                .andExpect(jsonPath("$.equipamentos[1].editada").value(true));
        getComo(dono.token(), url)
                .andExpect(jsonPath("$.equipamentos[0].catalogKey").value("longsword"))
                .andExpect(jsonPath("$.equipamentos[1].nome").value("Espada do avô"));
        getComo(dono.token(), URL + "/longsword").andExpect(jsonPath("$.nome").value("Espada longa"));

        putComo(registrar("JOGADOR").token(), url, abaDeEquipamento(List.of(espadaLongaDoCatalogo("Invasão", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarItemDoCatalogoQueNaoCombinaComOTipoDoEquipamento() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        Map<String, Object> mochilaComoArma = new HashMap<>(espadaLongaDoCatalogo("Mochila", 2.5));
        mochilaComoArma.put("catalogKey", "backpack");
        Map<String, Object> chaveInexistente = new HashMap<>(espadaLongaDoCatalogo("Espada", 1.5));
        chaveInexistente.put("catalogKey", "espada-inventada");
        Map<String, Object> pesoNegativo = new HashMap<>(espadaLongaDoCatalogo("Espada longa", -1));

        for (Map<String, Object> invalido : List.of(mochilaComoArma, chaveInexistente, pesoNegativo)) {
            putComo(dono.token(), "/api/characters/" + fichaId + "/ficha/equipamento",
                    abaDeEquipamento(List.of(invalido)))
                    .andExpect(status().isBadRequest());
        }
    }

    private Map<String, Object> abaDeItens(List<Map<String, Object>> itens) {
        return Map.of("equipamentos", List.of(), "items", itens);
    }

    @Test
    void deveGuardarItensDoCatalogoComQuantidadeEPesoPorUnidade() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");
        String url = "/api/characters/" + fichaId + "/ficha/equipamento";

        putComo(dono.token(), url, abaDeItens(List.of(
                Map.of("nome", "Flechas", "quantidade", 40, "pesoKg", 0.025, "catalogKey", "arrow"),
                Map.of("nome", "Mochila velha", "pesoKg", 2.5, "catalogKey", "backpack"),
                Map.of("nome", "Amuleto da avó", "quantidade", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantidade").value(40))
                .andExpect(jsonPath("$.items[0].pesoKg").value(0.025))
                .andExpect(jsonPath("$.items[0].editado").value(false))
                .andExpect(jsonPath("$.items[1].quantidade").value(1))
                .andExpect(jsonPath("$.items[1].editado").value(true))
                .andExpect(jsonPath("$.items[2].catalogKey").doesNotExist());
        getComo(dono.token(), url)
                .andExpect(jsonPath("$.items[0].catalogKey").value("arrow"))
                .andExpect(jsonPath("$.items[0].nome").value("Flechas"));

        putComo(registrar("JOGADOR").token(), url, abaDeItens(List.of(Map.of("nome", "Invasão"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarItemComQuantidadePesoOuChaveDoCatalogoInvalidos() throws Exception {
        Conta dono = registrar("JOGADOR");
        Long fichaId = criarPersonagem(dono, "PLAYER");

        for (Map<String, Object> invalido : List.<Map<String, Object>>of(
                Map.of("nome", "Tocha", "quantidade", 0),
                Map.of("nome", "Tocha", "quantidade", 10000),
                Map.of("nome", "Tocha", "pesoKg", -1),
                Map.of("nome", "Tocha", "catalogKey", "tocha-inventada"),
                Map.of("nome", " "))) {
            putComo(dono.token(), "/api/characters/" + fichaId + "/ficha/equipamento", abaDeItens(List.of(invalido)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void deveNegarAcessoSemLoginENaoPermitirAlterarOCatalogo() throws Exception {
        Conta jogador = registrar("JOGADOR");

        getComo(null, URL + "?nome=espada").andExpect(status().isUnauthorized());
        getComo(null, URL + "/longsword").andExpect(status().isUnauthorized());
        postComo(jogador.token(), URL, Map.of("nome", "Nova")).andExpect(status().isMethodNotAllowed());
        putComo(jogador.token(), URL + "/longsword", Map.of("nome", "Espada")).andExpect(status().isMethodNotAllowed());
        deleteComo(jogador.token(), URL + "/longsword").andExpect(status().isMethodNotAllowed());
    }
}
