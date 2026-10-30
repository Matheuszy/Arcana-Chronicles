package com.arcana.backend;

import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.config.security.LimiteDeTentativasFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LimiteDeTentativasFilterTest {

    private LimiteDeTentativasFilter filtro;
    private FilterChain cadeia;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        filtro = new LimiteDeTentativasFilter(new JacksonConfig().objectMapper(), relogio,
                3, Duration.ofMinutes(1), 2, Duration.ofMinutes(10));
        cadeia = mock(FilterChain.class);
    }

    private MockHttpServletResponse chamar(String metodo, String rota, String ip) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, rota);
        requisicao.setRemoteAddr(ip);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, cadeia);
        return resposta;
    }

    @Test
    void deveRecusarLoginComStatus429AposExcederAsTentativas() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, chamar("POST", "/api/users/login", "1.1.1.1").getStatus());
        }

        MockHttpServletResponse bloqueada = chamar("POST", "/api/users/login", "1.1.1.1");

        assertEquals(429, bloqueada.getStatus());
        assertEquals("60", bloqueada.getHeader("Retry-After"));
        assertTrue(bloqueada.getContentAsString().contains(LimiteDeTentativasFilter.MENSAGEM));
        verify(cadeia, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveLimitarOCadastroComSeuProprioContador() throws Exception {
        chamar("POST", "/api/users/register", "1.1.1.1");
        chamar("POST", "/api/users/register", "1.1.1.1");

        assertEquals(429, chamar("POST", "/api/users/register", "1.1.1.1").getStatus());
        assertEquals(200, chamar("POST", "/api/users/login", "1.1.1.1").getStatus());
    }

    @Test
    void deveContarCadaEnderecoSeparadamente() throws Exception {
        for (int i = 0; i < 4; i++) {
            chamar("POST", "/api/users/login", "1.1.1.1");
        }

        assertEquals(200, chamar("POST", "/api/users/login", "2.2.2.2").getStatus());
    }

    @Test
    void naoDeveLimitarOutrasRotasNemOutrosMetodos() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, chamar("GET", "/api/users/login", "1.1.1.1").getStatus());
            assertEquals(200, chamar("POST", "/api/characters", "1.1.1.1").getStatus());
        }
        verify(cadeia, times(20)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
