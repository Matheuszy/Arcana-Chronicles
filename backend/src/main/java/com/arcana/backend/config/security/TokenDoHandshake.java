package com.arcana.backend.config.security;

import org.springframework.http.server.ServerHttpRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class TokenDoHandshake {

    public static final String PROTOCOLO_AUTENTICACAO = "arcana.jwt";

    private static final String CABECALHO_PROTOCOLO = "Sec-WebSocket-Protocol";

    private TokenDoHandshake() {}

    public static Optional<String> extrair(ServerHttpRequest request) {
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
