package com.arcana.backend;

import com.arcana.backend.config.security.SecurityFilter;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        securityFilter = new SecurityFilter(tokenService, usuarioRepositorie);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveAutenticarQuandoTokenForValido() throws ServletException, IOException {
        String token = "valid-token";
        Usuario usuario = new Usuario("aragorn", "aragorn@arcana.com", "pass");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.validarToken(token)).thenReturn("aragorn");
        when(usuarioRepositorie.findByUsername("aragorn")).thenReturn(Optional.of(usuario));

        securityFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("aragorn", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoHeaderAuthorizationAusente() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void naoDeveAutenticarQuandoTokenForInvalido() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(tokenService.validarToken("token-invalido")).thenReturn(null);

        securityFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
