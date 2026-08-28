package com.arcana.backend;

import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "teste-secret-key-12345678901234567890");
        ReflectionTestUtils.setField(tokenService, "expirationMillis", 3600000L);
    }

    @Test
    void deveGerarEValidarTokenComSucesso() {
        Usuario usuario = new Usuario("gandalf", "gandalf@arcana.com", "hash");
        usuario.setId(10L);

        String token = tokenService.gerarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String subject = tokenService.validarToken(token);
        assertEquals("gandalf", subject);
    }

    @Test
    void deveRetornarNullParaTokenInvalido() {
        String subject = tokenService.validarToken("token-totalmente-invalido");
        assertNull(subject);
    }

    @Test
    void deveRetornarNullParaTokenNuloOuVazio() {
        assertNull(tokenService.validarToken(null));
        assertNull(tokenService.validarToken(""));
        assertNull(tokenService.validarToken("   "));
    }
}
