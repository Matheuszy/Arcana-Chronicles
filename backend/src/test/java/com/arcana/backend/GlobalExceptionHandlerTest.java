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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    void deveRetornar404ParaRotaInexistente() {
        var response = handler.handleRotaInexistente(
                new NoResourceFoundException(HttpMethod.POST, "/api/tables/1/entrar", "api/tables/1/entrar"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deveRetornar405ParaMetodoNaoSuportado() {
        var response = handler.handleMetodoNaoSuportado(
                new HttpRequestMethodNotSupportedException("PUT"), request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void deveRetornar400ParaCorpoInvalido() {
        var response = handler.handleCorpoIlegivel(
                new HttpMessageNotReadableException("JSON inválido", new MockHttpInputMessage(new byte[0])), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void deveManterStatusDeErroDoClienteVindoDoFramework() {
        var response = handler.handleGenerico(
                new HttpMediaTypeNotSupportedException("text/plain"), request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
    }

    @Test
    void deveRetornar500GenericoSemExporDetalhesInternos() {
        var response = handler.handleGenerico(new RuntimeException("stacktrace sensível"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Erro interno no servidor.", response.getBody().message());
    }
}
