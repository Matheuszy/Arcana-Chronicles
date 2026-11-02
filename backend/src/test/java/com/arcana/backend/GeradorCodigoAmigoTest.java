package com.arcana.backend;

import com.arcana.backend.user.model.CodigoAmigo;
import com.arcana.backend.user.service.GeradorCodigoAmigo;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorCodigoAmigoTest {

    private final GeradorCodigoAmigo gerador = new GeradorCodigoAmigo();

    @Test
    void deveGerarCodigoComOitoCaracteresDoAlfabetoSemAmbiguidade() {
        for (int i = 0; i < 500; i++) {
            String codigo = gerador.gerar();

            assertEquals(CodigoAmigo.TAMANHO, codigo.length());
            assertTrue(codigo.chars().allMatch(c -> CodigoAmigo.ALFABETO.indexOf(c) >= 0), codigo);
        }
    }

    @Test
    void naoDeveUsarCaracteresAmbiguos() {
        assertTrue("01IO".chars().noneMatch(c -> CodigoAmigo.ALFABETO.indexOf(c) >= 0));
        assertEquals(32, CodigoAmigo.ALFABETO.chars().distinct().count());
    }

    @Test
    void deveGerarCodigosDiferentesEmSequencia() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codigos.add(gerador.gerar());
        }

        assertEquals(1000, codigos.size());
    }

    @Test
    void deveFormatarCodigoComPrefixoEHifens() {
        assertEquals("ARC-7K9M-X2QP", CodigoAmigo.formatar("7K9MX2QP"));
        assertNull(CodigoAmigo.formatar(null));
    }
}
