package com.arcana.backend.chat;

import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AutenticacaoHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PROTOCOLO_AUTENTICACAO = "arcana.jwt";
    public static final String ATRIBUTO_ACESSO = "acessoMesa";

    private static final String CABECALHO_PROTOCOLO = "Sec-WebSocket-Protocol";
    private static final Logger log = LoggerFactory.getLogger(AutenticacaoHandshakeInterceptor.class);

    private final TokenService tokenService;
    private final UsuarioRepositorie usuarioRepositorie;
    private final MesaService mesaService;

    public AutenticacaoHandshakeInterceptor(TokenService tokenService,
                                            UsuarioRepositorie usuarioRepositorie,
                                            MesaService mesaService) {
        this.tokenService = tokenService;
        this.usuarioRepositorie = usuarioRepositorie;
        this.mesaService = mesaService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Optional<Long> mesaId = extrairMesaId(request);
        if (mesaId.isEmpty()) {
            return recusar(response, HttpStatus.BAD_REQUEST, null, null, "mesa inválida");
        }

        Optional<Usuario> usuario = extrairToken(request)
                .map(tokenService::validarToken)
                .flatMap(usuarioRepositorie::findByUsername);
        if (usuario.isEmpty()) {
            return recusar(response, HttpStatus.UNAUTHORIZED, mesaId.get(), null, "token ausente ou inválido");
        }

        Long userId = usuario.get().getId();
        try {
            AcessoMesaDto acesso = mesaService.validarAcessoAoChat(mesaId.get(), userId);
            attributes.put(ATRIBUTO_ACESSO, acesso);
            return true;
        } catch (RecursoNaoEncontradoException e) {
            return recusar(response, HttpStatus.NOT_FOUND, mesaId.get(), userId, e.getMessage());
        } catch (AccessDeniedException | RegraDeNegocioException e) {
            return recusar(response, HttpStatus.FORBIDDEN, mesaId.get(), userId, e.getMessage());
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private boolean recusar(ServerHttpResponse response, HttpStatus status, Long mesaId, Long userId, String motivo) {
        log.warn("Conexão ao chat recusada: mesa={} usuario={} status={} motivo={}", mesaId, userId, status.value(), motivo);
        response.setStatusCode(status);
        return false;
    }

    private Optional<Long> extrairMesaId(ServerHttpRequest request) {
        String[] partes = request.getURI().getPath().split("/");
        try {
            return Optional.of(Long.parseLong(partes[partes.length - 1]));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private Optional<String> extrairToken(ServerHttpRequest request) {
        List<String> protocolos = request.getHeaders().getOrEmpty(CABECALHO_PROTOCOLO).stream()
                .flatMap(valor -> Arrays.stream(valor.split(",")))
                .map(String::trim)
                .toList();

        int indice = protocolos.indexOf(PROTOCOLO_AUTENTICACAO);
        if (indice < 0 || indice + 1 >= protocolos.size()) {
            return Optional.empty();
        }
        return Optional.of(protocolos.get(indice + 1)).filter(token -> !token.isBlank());
    }
}
