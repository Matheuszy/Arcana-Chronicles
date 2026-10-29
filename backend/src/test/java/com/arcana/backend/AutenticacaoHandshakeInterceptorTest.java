package com.arcana.backend;

import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticacaoHandshakeInterceptorTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    @Mock
    private MesaService mesaService;

    private AutenticacaoHandshakeInterceptor interceptor;
    private MockHttpServletResponse servletResponse;
    private final Map<String, Object> atributos = new HashMap<>();

    @BeforeEach
    void setUp() {
        interceptor = new AutenticacaoHandshakeInterceptor(tokenService, usuarioRepositorie, mesaService);
        servletResponse = new MockHttpServletResponse();
    }

    private boolean conectar(String caminho, String protocolos) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", caminho);
        if (protocolos != null) {
            servletRequest.addHeader("Sec-WebSocket-Protocol", protocolos);
        }
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                null,
                atributos);
    }

    private Usuario joao() {
        Usuario usuario = new Usuario("joao", "joao@arcana.com", "hash", PapelUsuario.JOGADOR);
        usuario.setId(2L);
        return usuario;
    }

    @Test
    void deveRecusarConexaoSemToken() {
        assertFalse(conectar("/ws/mesas/1", null));
        assertEquals(401, servletResponse.getStatus());
        verifyNoInteractions(mesaService);
    }

    @Test
    void deveRecusarConexaoComTokenInvalido() {
        when(tokenService.validarToken("token-falso")).thenReturn(null);

        assertFalse(conectar("/ws/mesas/1", "arcana.jwt, token-falso"));
        assertEquals(401, servletResponse.getStatus());
        verifyNoInteractions(mesaService);
    }

    @Test
    void deveRecusarUsuarioQueNaoParticipaDaMesa() {
        when(tokenService.validarToken("token-joao")).thenReturn("joao");
        when(usuarioRepositorie.findByUsername("joao")).thenReturn(Optional.of(joao()));
        when(mesaService.validarAcessoAoChat(1L, 2L))
                .thenThrow(new AccessDeniedException("Apenas participantes da mesa podem entrar no chat."));

        assertFalse(conectar("/ws/mesas/1", "arcana.jwt, token-joao"));
        assertEquals(403, servletResponse.getStatus());
        assertTrue(atributos.isEmpty());
    }

    @Test
    void deveRecusarMesaEncerrada() {
        when(tokenService.validarToken("token-joao")).thenReturn("joao");
        when(usuarioRepositorie.findByUsername("joao")).thenReturn(Optional.of(joao()));
        when(mesaService.validarAcessoAoChat(1L, 2L)).thenThrow(new RegraDeNegocioException("A mesa está encerrada."));

        assertFalse(conectar("/ws/mesas/1", "arcana.jwt, token-joao"));
        assertEquals(403, servletResponse.getStatus());
    }

    @Test
    void deveRecusarMesaInexistente() {
        when(tokenService.validarToken("token-joao")).thenReturn("joao");
        when(usuarioRepositorie.findByUsername("joao")).thenReturn(Optional.of(joao()));
        when(mesaService.validarAcessoAoChat(99L, 2L)).thenThrow(new RecursoNaoEncontradoException("Mesa não encontrada: 99"));

        assertFalse(conectar("/ws/mesas/99", "arcana.jwt, token-joao"));
        assertEquals(404, servletResponse.getStatus());
    }

    @Test
    void deveRecusarIdentificadorDeMesaInvalido() {
        assertFalse(conectar("/ws/mesas/abc", "arcana.jwt, token-joao"));
        assertEquals(400, servletResponse.getStatus());
        verify(tokenService, never()).validarToken(any());
    }

    @Test
    void deveAceitarParticipanteEGuardarIdentidadeNaSessao() {
        AcessoMesaDto acesso = new AcessoMesaDto(1L, 2L, "joao", "JOGADOR");
        when(tokenService.validarToken("token-joao")).thenReturn("joao");
        when(usuarioRepositorie.findByUsername("joao")).thenReturn(Optional.of(joao()));
        when(mesaService.validarAcessoAoChat(1L, 2L)).thenReturn(acesso);

        assertTrue(conectar("/ws/mesas/1", "arcana.jwt, token-joao"));
        assertEquals(acesso, atributos.get(AutenticacaoHandshakeInterceptor.ATRIBUTO_ACESSO));
    }
}
