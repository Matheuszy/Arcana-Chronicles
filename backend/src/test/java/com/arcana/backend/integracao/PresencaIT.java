package com.arcana.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PresencaIT extends IntegracaoTest {

    private JsonNode aguardarPresenca(ClienteChat cliente, Conta amigo, String estado) throws InterruptedException {
        while (true) {
            JsonNode mensagem = cliente.aguardar("PRESENCA");
            JsonNode payload = mensagem.get("payload");
            if (payload.get("usuarioId").asLong() == amigo.id() && estado.equals(payload.get("estado").asText())) {
                return payload;
            }
        }
    }

    @Test
    void deveRecusarCanalDoUsuarioSemTokenValido() throws Exception {
        assertEquals(401, statusDoHandshakeDoCanalDoUsuario(null));
        assertEquals(401, statusDoHandshakeDoCanalDoUsuario("token.invalido"));
        assertEquals(101, statusDoHandshakeDoCanalDoUsuario(registrar("JOGADOR").token()));
    }

    @Test
    void deveMostrarAosAmigosQuandoOUsuarioFicaOnlineEntraNaMesaEFicaOffline() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        Conta amiga = registrar("JOGADOR");
        tornarAmigos(amiga, jogador.conta());

        ClienteChat canalDaAmiga = conectarAoCanalDoUsuario(amiga.token());
        ClienteChat canalDoJogador = conectarAoCanalDoUsuario(jogador.conta().token());
        aguardarPresenca(canalDaAmiga, jogador.conta(), "ONLINE");
        getComo(amiga.token(), "/api/amigos").andExpect(jsonPath("$[0].presenca").value("ONLINE"));

        ClienteChat chatDaMesa = conectarAoChat(mesaId, jogador.conta().token());
        aguardarPresenca(canalDaAmiga, jogador.conta(), "EM_MESA");
        getComo(amiga.token(), "/api/amigos").andExpect(jsonPath("$[0].presenca").value("EM_MESA"));

        chatDaMesa.fechar();
        aguardarPresenca(canalDaAmiga, jogador.conta(), "ONLINE");

        canalDoJogador.fechar();
        aguardarPresenca(canalDaAmiga, jogador.conta(), "OFFLINE");
        canalDaAmiga.fechar();
    }

    @Test
    void naoDeveMostrarPresencaParaQuemNaoEAmigoNemParaQuemFoiBloqueado() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        Conta estranho = registrar("JOGADOR");
        tornarAmigos(aria, brom);
        postComo(brom.token(), "/api/amigos/bloqueios", Map.of("usuarioId", aria.id())).andExpect(status().isCreated());

        ClienteChat canalDoEstranho = conectarAoCanalDoUsuario(estranho.token());
        ClienteChat canalDaAria = conectarAoCanalDoUsuario(aria.token());
        ClienteChat canalDoBrom = conectarAoCanalDoUsuario(brom.token());

        canalDaAria.enviar(Map.of("type", "PING"));
        getComo(aria.token(), "/api/amigos").andExpect(jsonPath("$.length()").value(0));
        assertSemPresenca(canalDaAria);
        assertSemPresenca(canalDoEstranho);

        canalDaAria.fechar();
        canalDoBrom.fechar();
        canalDoEstranho.fechar();
    }

    private void assertSemPresenca(ClienteChat cliente) throws InterruptedException {
        try {
            cliente.aguardar("PRESENCA");
        } catch (AssertionError esperado) {
            return;
        }
        throw new AssertionError("Recebeu presença de quem não é amigo.");
    }
}
