package com.arcana.backend;

import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.config.security.AutenticacaoEntryPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacaoEntryPointTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final AutenticacaoEntryPoint entryPoint = new AutenticacaoEntryPoint(objectMapper);

    @Test
    void deveResponderNaoAutorizadoEmJsonParaRequisicaoSemAutenticacao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tables");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("sem token"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode corpo = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(401, corpo.get("status").asInt());
        assertEquals(AutenticacaoEntryPoint.MENSAGEM, corpo.get("message").asText());
        assertEquals("/api/tables", corpo.get("path").asText());
    }
}
