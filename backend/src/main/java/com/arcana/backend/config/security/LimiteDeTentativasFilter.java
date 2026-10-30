package com.arcana.backend.config.security;

import com.arcana.backend.config.exception.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Component
public class LimiteDeTentativasFilter extends OncePerRequestFilter {

    public static final String MENSAGEM = "Muitas tentativas. Aguarde um pouco e tente novamente.";

    private static final String ROTA_LOGIN = "/api/users/login";
    private static final String ROTA_CADASTRO = "/api/users/register";

    private final ObjectMapper objectMapper;
    private final LimitadorDeTentativas limitadorDeLogin;
    private final LimitadorDeTentativas limitadorDeCadastro;

    public LimiteDeTentativasFilter(ObjectMapper objectMapper,
                                    Clock clock,
                                    @Value("${arcana.seguranca.login-maximo-tentativas}") int loginMaximo,
                                    @Value("${arcana.seguranca.login-janela}") Duration loginJanela,
                                    @Value("${arcana.seguranca.cadastro-maximo-tentativas}") int cadastroMaximo,
                                    @Value("${arcana.seguranca.cadastro-janela}") Duration cadastroJanela) {
        this.objectMapper = objectMapper;
        this.limitadorDeLogin = new LimitadorDeTentativas(loginMaximo, loginJanela, clock);
        this.limitadorDeCadastro = new LimitadorDeTentativas(cadastroMaximo, cadastroJanela, clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || limitadorDaRota(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorDeTentativas limitador = limitadorDaRota(request);
        if (limitador.permitir(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;
        response.setStatus(status.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limitador.janela().toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ApiErrorResponse.of(status.value(), status.getReasonPhrase(), MENSAGEM, request.getRequestURI()));
    }

    private LimitadorDeTentativas limitadorDaRota(HttpServletRequest request) {
        return switch (request.getRequestURI()) {
            case ROTA_LOGIN -> limitadorDeLogin;
            case ROTA_CADASTRO -> limitadorDeCadastro;
            default -> null;
        };
    }
}
