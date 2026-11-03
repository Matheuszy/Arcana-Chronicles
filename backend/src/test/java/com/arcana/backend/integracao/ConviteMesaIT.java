package com.arcana.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConviteMesaIT extends IntegracaoTest {

    private Long convidar(Conta remetente, Long mesaId, Conta convidado) throws Exception {
        return corpo(postComo(remetente.token(), "/api/tables/" + mesaId + "/convites",
                Map.of("convidadoId", convidado.id()))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @Test
    void deveIncluirNaMesaQuemAceitaConviteDoMestre() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");
        tornarAmigos(mestre, jogador);
        ClienteChat canal = conectarAoCanalDoUsuario(jogador.token());

        Long conviteId = convidar(mestre, mesaId, jogador);

        JsonNode recebido = canal.aguardar("CONVITE_RECEBIDO").get("payload");
        assertEquals(conviteId, recebido.get("id").asLong());
        assertTrue(recebido.get("convitePeloMestre").asBoolean());
        getComo(jogador.token(), "/api/convites").andExpect(jsonPath("$[0].id").value(conviteId));

        postComo(jogador.token(), "/api/convites/" + conviteId + "/aceitar", Map.of("characterId", personagemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrouNaMesa").value(true))
                .andExpect(jsonPath("$.convite.status").value("ACEITO"));

        String participantes = corpo(getComo(mestre.token(), "/api/tables/" + mesaId)).get("participants").toString();
        assertTrue(participantes.contains(jogador.username()));
        getComo(jogador.token(), "/api/convites").andExpect(jsonPath("$", hasSize(0)));
        canal.fechar();
    }

    @Test
    void deveCriarSolicitacaoPendenteQuandoAceitaConviteDeJogador() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa participante = jogadorAceitoNaMesa(mestre, mesaId);
        Conta amigo = registrar("JOGADOR");
        Long personagemId = criarPersonagem(amigo, "PLAYER");
        tornarAmigos(participante.conta(), amigo);
        ClienteChat canal = conectarAoCanalDoUsuario(amigo.token());

        Long conviteId = convidar(participante.conta(), mesaId, amigo);
        assertTrue(!canal.aguardar("CONVITE_RECEBIDO").get("payload").get("convitePeloMestre").asBoolean());

        postComo(amigo.token(), "/api/convites/" + conviteId + "/aceitar", Map.of("characterId", personagemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrouNaMesa").value(false));

        getComo(mestre.token(), "/api/tables/" + mesaId + "/solicitacoes")
                .andExpect(jsonPath("$[?(@.userId == " + amigo.id() + ")].status").value("PENDENTE"));
        String participantes = corpo(getComo(mestre.token(), "/api/tables/" + mesaId)).get("participants").toString();
        assertTrue(!participantes.contains(amigo.username()));
        canal.fechar();
    }

    @Test
    void deveNegarConviteDeQuemNaoParticipaEParaQuemNaoPodeSerConvidado() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Conta estranho = registrar("JOGADOR");
        Conta outroMestre = registrar("MESTRE");
        tornarAmigos(estranho, jogador);
        tornarAmigos(mestre, outroMestre);
        tornarAmigos(mestre, jogador);
        ClienteChat canalDoJogador = conectarAoCanalDoUsuario(jogador.token());
        ClienteChat canalDoOutroMestre = conectarAoCanalDoUsuario(outroMestre.token());

        postComo(null, "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", jogador.id()))
                .andExpect(status().isUnauthorized());
        postComo(estranho.token(), "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", jogador.id()))
                .andExpect(status().isForbidden());
        postComo(mestre.token(), "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", estranho.id()))
                .andExpect(status().isConflict());
        postComo(mestre.token(), "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", outroMestre.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Contas de mestre não entram em mesas como jogador."));

        convidar(mestre, mesaId, jogador);
        postComo(mestre.token(), "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", jogador.id()))
                .andExpect(status().isConflict());
        canalDoJogador.fechar();
        canalDoOutroMestre.fechar();
    }

    @Test
    void deveRecusarConviteParaAmigoOffline() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        tornarAmigos(mestre, jogador);

        postComo(mestre.token(), "/api/tables/" + mesaId + "/convites", Map.of("convidadoId", jogador.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este amigo não está online."));
    }

    @Test
    void devePermitirResponderApenasAoConvidado() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Conta intruso = registrar("JOGADOR");
        Long personagemDoIntruso = criarPersonagem(intruso, "PLAYER");
        tornarAmigos(mestre, jogador);
        ClienteChat canal = conectarAoCanalDoUsuario(jogador.token());
        Long conviteId = convidar(mestre, mesaId, jogador);

        postComo(intruso.token(), "/api/convites/" + conviteId + "/aceitar", Map.of("characterId", personagemDoIntruso))
                .andExpect(status().isForbidden());
        postComo(intruso.token(), "/api/convites/" + conviteId + "/recusar", null)
                .andExpect(status().isForbidden());
        getComo(intruso.token(), "/api/convites").andExpect(jsonPath("$", hasSize(0)));

        postComo(jogador.token(), "/api/convites/" + conviteId + "/recusar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));
        postComo(jogador.token(), "/api/convites/" + conviteId + "/recusar", null)
                .andExpect(status().isConflict());
        canal.fechar();
    }

    @Test
    void deveExpirarConviteQuandoAMesaDeixaDeEstarAberta() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");
        tornarAmigos(mestre, jogador);
        ClienteChat canal = conectarAoCanalDoUsuario(jogador.token());
        Long conviteId = convidar(mestre, mesaId, jogador);

        patchComo(mestre.token(), "/api/tables/" + mesaId + "/status", Map.of("status", "EM_ANDAMENTO"))
                .andExpect(status().isOk());

        JsonNode encerrado = canal.aguardar("CONVITE_ENCERRADO").get("payload");
        assertEquals(conviteId, encerrado.get("conviteId").asLong());
        assertEquals("EXPIRADO", encerrado.get("status").asText());
        postComo(jogador.token(), "/api/convites/" + conviteId + "/aceitar", Map.of("characterId", personagemId))
                .andExpect(status().isConflict());
        canal.fechar();
    }

    @Test
    void deveExpirarConviteQuandoOConvidadoFicaOffline() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        Conta jogador = registrar("JOGADOR");
        tornarAmigos(mestre, jogador);
        ClienteChat canal = conectarAoCanalDoUsuario(jogador.token());
        convidar(mestre, mesaId, jogador);

        canal.fechar();

        long limite = System.currentTimeMillis() + 5000;
        int pendentes;
        do {
            pendentes = corpo(getComo(jogador.token(), "/api/convites")).size();
        } while (pendentes > 0 && System.currentTimeMillis() < limite);
        assertEquals(0, pendentes);
    }
}
