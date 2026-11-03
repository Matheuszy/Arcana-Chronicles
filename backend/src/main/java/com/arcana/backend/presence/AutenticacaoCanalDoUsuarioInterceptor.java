package com.arcana.backend.presence;

import com.arcana.backend.config.security.TokenDoHandshake;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
public class AutenticacaoCanalDoUsuarioInterceptor implements HandshakeInterceptor {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioIdDoCanal";

    private final TokenService tokenService;
    private final UsuarioRepositorie usuarioRepositorie;

    public AutenticacaoCanalDoUsuarioInterceptor(TokenService tokenService, UsuarioRepositorie usuarioRepositorie) {
        this.tokenService = tokenService;
        this.usuarioRepositorie = usuarioRepositorie;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Optional<Usuario> usuario = TokenDoHandshake.extrair(request)
                .map(tokenService::validarToken)
                .flatMap(usuarioRepositorie::findByUsername);
        if (usuario.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATRIBUTO_USUARIO_ID, usuario.get().getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
