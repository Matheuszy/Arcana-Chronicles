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
    private static final String ROTA_BUSCA_DE_AMIGOS = "/api/amigos/busca";

    private final ObjectMapper objectMapper;
    private final LimitadorDeTentativas limitadorDeLogin;
    private final LimitadorDeTentativas limitadorDeCadastro;
    private final LimitadorDeTentativas limitadorDeBuscaDeAmigos;

    public LimiteDeTentativasFilter(ObjectMapper objectMapper,
                                    Clock clock,
                                    @Value("${arcana.seguranca.login-maximo-tentativas}") int loginMaximo,
                                    @Value("${arcana.seguranca.login-janela}") Duration loginJanela,
                                    @Value("${arcana.seguranca.cadastro-maximo-tentativas}") int cadastroMaximo,
                                    @Value("${arcana.seguranca.cadastro-janela}") Duration cadastroJanela,
                                    @Value("${arcana.seguranca.busca-amigos-maximo-tentativas}") int buscaMaximo,
                                    @Value("${arcana.seguranca.busca-amigos-janela}") Duration buscaJanela) {
        this.objectMapper = objectMapper;
        this.limitadorDeLogin = new LimitadorDeTentativas(loginMaximo, loginJanela, clock);
        this.limitadorDeCadastro = new LimitadorDeTentativas(cadastroMaximo, cadastroJanela, clock);
        this.limitadorDeBuscaDeAmigos = new LimitadorDeTentativas(buscaMaximo, buscaJanela, clock);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return limitadorDaRota(request) == null;
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
        if (HttpMethod.GET.matches(request.getMethod())) {
            return ROTA_BUSCA_DE_AMIGOS.equals(request.getRequestURI()) ? limitadorDeBuscaDeAmigos : null;
        }
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        return switch (request.getRequestURI()) {
            case ROTA_LOGIN -> limitadorDeLogin;
            case ROTA_CADASTRO -> limitadorDeCadastro;
            default -> null;
        };
    }
}
