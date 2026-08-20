package com.arcana.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuração global de CORS — substitui os @CrossOrigin individuais nos controllers.
 * Em produção, trocar "http://localhost:5173" pelo domínio real do frontend.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origens permitidas
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers que o frontend pode enviar (inclui os temporários X-Owner-Id e X-Display-Name)
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Owner-Id",
                "X-Display-Name"
        ));

        // Headers que o frontend pode ler na resposta
        config.setExposedHeaders(List.of("Authorization"));

        // Permite cookies/credenciais (necessário quando JWT for implementado)
        config.setAllowCredentials(true);

        // Cache do preflight por 1 hora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
