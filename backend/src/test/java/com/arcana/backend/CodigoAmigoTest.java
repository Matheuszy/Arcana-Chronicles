package com.arcana.backend;

import com.arcana.backend.user.model.CodigoAmigo;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodigoAmigoTest {

    @Test
    void deveNormalizarOCodigoEmQualquerFormatacao() {
        assertEquals(Optional.of("7K9MX2QP"), CodigoAmigo.normalizar("ARC-7K9M-X2QP"));
        assertEquals(Optional.of("7K9MX2QP"), CodigoAmigo.normalizar("arc 7k9m x2qp"));
        assertEquals(Optional.of("7K9MX2QP"), CodigoAmigo.normalizar("7k9m-x2qp"));
        assertEquals(Optional.of("7K9MX2QP"), CodigoAmigo.normalizar("  7K9MX2QP  "));
    }

    @Test
    void deveIgnorarTextoQueNaoEUmCodigoDeAmigo() {
        assertEquals(Optional.empty(), CodigoAmigo.normalizar("aria"));
        assertEquals(Optional.empty(), CodigoAmigo.normalizar("ARC-7K9M-X2Q0"));
        assertEquals(Optional.empty(), CodigoAmigo.normalizar("7K9MX2QPZ"));
        assertEquals(Optional.empty(), CodigoAmigo.normalizar(null));
    }
}
