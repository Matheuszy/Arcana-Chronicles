package com.arcana.backend;

import com.arcana.backend.config.exception.CredenciaisInvalidasException;
import com.arcana.backend.config.exception.GlobalExceptionHandler;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/teste");
    }

    @Test
    void deveRetornar404ParaRecursoNaoEncontrado() {
        var response = handler.handleNaoEncontrado(
                new RecursoNaoEncontradoException("Mesa não encontrada: 1"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Mesa não encontrada: 1", response.getBody().message());
    }

    @Test
    void deveRetornar403ParaAcessoNegado() {
        var response = handler.handleAcessoNegado(
                new AccessDeniedException("Acesso negado"), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deveRetornar409ParaRegraDeNegocio() {
        var response = handler.handleRegraDeNegocio(
                new RegraDeNegocioException("A mesa não está aberta."), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void deveRetornar401ParaCredenciaisInvalidas() {
        var response = handler.handleCredenciaisInvalidas(
                new CredenciaisInvalidasException("Usuário ou senha inválidos."), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deveRetornar500GenericoSemExporDetalhesInternos() {
        var response = handler.handleGenerico(new RuntimeException("stacktrace sensível"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno no servidor.", response.getBody().message());
    }
}
