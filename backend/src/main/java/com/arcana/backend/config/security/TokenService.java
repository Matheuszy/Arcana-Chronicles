package com.arcana.backend.config.security;

import com.arcana.backend.user.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenService {

    static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration:86400000}")
    private Long expirationMillis;

    @PostConstruct
    public void validarSegredo() {
        if (secret == null || secret.length() < TAMANHO_MINIMO_DO_SEGREDO) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ser definido com pelo menos " + TAMANHO_MINIMO_DO_SEGREDO + " caracteres.");
        }
    }

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("arcana-backend")
                    .withSubject(usuario.getUsername())
                    .withClaim("id", usuario.getId())
                    .withClaim("email", usuario.getEmail())
                    .withExpiresAt(new Date(System.currentTimeMillis() + expirationMillis))
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validarToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("arcana-backend")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}
