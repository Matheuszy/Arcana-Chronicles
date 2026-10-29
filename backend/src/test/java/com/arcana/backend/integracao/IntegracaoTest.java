package com.arcana.backend.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegracaoTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final Duration ESPERA_MENSAGEM = Duration.ofSeconds(5);

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected MockMvc mockMvc;

    @LocalServerPort
    private int porta;

    protected record Conta(Long id, String username, String token) {}

    protected record JogadorNaMesa(Conta conta, Long personagemId) {}

    protected String nomeUnico(String prefixo) {
        return prefixo + SEQUENCIA.incrementAndGet() + "_" + System.nanoTime() % 100_000;
    }

    protected String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    protected JsonNode corpo(ResultActions resultado) throws Exception {
        String conteudo = resultado.andReturn().getResponse().getContentAsString();
        return conteudo.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(conteudo);
    }

    protected ResultActions executar(MockHttpServletRequestBuilder requisicao, String token, Object corpo) throws Exception {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(json(corpo));
        }
        return mockMvc.perform(requisicao);
    }

    protected ResultActions getComo(String token, String url) throws Exception {
        return executar(get(url), token, null);
    }

    protected ResultActions postComo(String token, String url, Object corpo) throws Exception {
        return executar(post(url), token, corpo);
    }

    protected ResultActions putComo(String token, String url, Object corpo) throws Exception {
        return executar(put(url), token, corpo);
    }

    protected ResultActions patchComo(String token, String url, Object corpo) throws Exception {
        return executar(patch(url), token, corpo);
    }

    protected ResultActions deleteComo(String token, String url) throws Exception {
        return executar(delete(url), token, null);
    }

    protected Conta registrar(String papel) throws Exception {
        String username = nomeUnico(papel.toLowerCase());
        postComo(null, "/api/users/register", Map.of(
                "username", username,
                "email", username + "@arcana.test",
                "password", "senha-secreta",
                "papel", papel))
                .andExpect(status().isCreated());

        JsonNode login = corpo(postComo(null, "/api/users/login", Map.of(
                "username", username, "password", "senha-secreta"))
                .andExpect(status().isOk()));
        return new Conta(login.get("id").asLong(), username, login.get("token").asText());
    }

    protected Map<String, Object> fichaDoTipo(String kind) {
        return Map.of(
                "name", nomeUnico("heroi"),
                "kind", kind,
                "level", 1,
                "hpMax", 10,
                "hpCurrent", 10,
                "armorClass", 12,
                "attributes", Map.of(
                        "forca", 10, "destreza", 12, "constituicao", 10,
                        "inteligencia", 10, "sabedoria", 10, "carisma", 10));
    }

    protected Long criarPersonagem(Conta dono, String kind) throws Exception {
        return corpo(postComo(dono.token(), "/api/characters", fichaDoTipo(kind))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    protected Long criarMesa(Conta mestre) throws Exception {
        return corpo(postComo(mestre.token(), "/api/tables", Map.of("name", nomeUnico("mesa"), "description", "teste"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    protected Long solicitarEntrada(Conta jogador, Long mesaId, Long personagemId) throws Exception {
        return corpo(postComo(jogador.token(), "/api/tables/" + mesaId + "/solicitacoes", Map.of("characterId", personagemId))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    protected JogadorNaMesa jogadorAceitoNaMesa(Conta mestre, Long mesaId) throws Exception {
        Conta jogador = registrar("JOGADOR");
        Long personagemId = criarPersonagem(jogador, "PLAYER");
        Long solicitacaoId = solicitarEntrada(jogador, mesaId, personagemId);
        postComo(mestre.token(), "/api/tables/" + mesaId + "/solicitacoes/" + solicitacaoId + "/aceitar", null)
                .andExpect(status().isOk());
        return new JogadorNaMesa(jogador, personagemId);
    }

    protected static final class ClienteChat implements WebSocket.Listener {

        private final BlockingQueue<JsonNode> recebidas = new LinkedBlockingQueue<>();
        private final StringBuilder parcial = new StringBuilder();
        private final ObjectMapper objectMapper;
        private volatile Integer codigoDeFechamento;
        private WebSocket socket;

        private ClienteChat(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence dados, boolean ultimo) {
            parcial.append(dados);
            if (ultimo) {
                try {
                    recebidas.add(objectMapper.readTree(parcial.toString()));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                parcial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int codigo, String motivo) {
            codigoDeFechamento = codigo;
            return null;
        }

        public void enviar(Map<String, Object> mensagem) throws Exception {
            socket.sendText(objectMapper.writeValueAsString(mensagem), true).get(5, TimeUnit.SECONDS);
        }

        public JsonNode aguardar(String tipo) throws InterruptedException {
            long limite = System.nanoTime() + ESPERA_MENSAGEM.toNanos();
            while (System.nanoTime() < limite) {
                JsonNode mensagem = recebidas.poll(limite - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (mensagem != null && tipo.equals(mensagem.path("type").asText())) {
                    return mensagem;
                }
            }
            throw new AssertionError("Mensagem do tipo " + tipo + " não chegou em " + ESPERA_MENSAGEM.toSeconds() + "s.");
        }

        public JsonNode aguardarConteudo(String conteudo) throws InterruptedException {
            long limite = System.nanoTime() + ESPERA_MENSAGEM.toNanos();
            while (System.nanoTime() < limite) {
                JsonNode mensagem = recebidas.poll(limite - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (mensagem != null && conteudo.equals(mensagem.path("content").asText())) {
                    return mensagem;
                }
            }
            throw new AssertionError("Mensagem \"" + conteudo + "\" não chegou em " + ESPERA_MENSAGEM.toSeconds() + "s.");
        }

        public Integer aguardarFechamento() throws InterruptedException {
            long limite = System.nanoTime() + ESPERA_MENSAGEM.toNanos();
            while (codigoDeFechamento == null && System.nanoTime() < limite) {
                Thread.sleep(50);
            }
            return codigoDeFechamento;
        }

        public void fechar() {
            if (socket != null && !socket.isOutputClosed()) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "fim do teste").exceptionally(erro -> null);
            }
        }
    }

    protected static final String ORIGEM_PERMITIDA = "http://localhost:4200";

    private WebSocket.Builder clienteWebSocket(String token, String origem) {
        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder().header("Origin", origem);
        return token == null ? builder : builder.subprotocols("arcana.jwt", token);
    }

    private URI enderecoDoChat(Long mesaId) {
        return URI.create("ws://localhost:" + porta + "/ws/mesas/" + mesaId);
    }

    protected ClienteChat conectarAoChat(Long mesaId, String token) throws Exception {
        ClienteChat cliente = new ClienteChat(objectMapper);
        cliente.socket = clienteWebSocket(token, ORIGEM_PERMITIDA)
                .buildAsync(enderecoDoChat(mesaId), cliente)
                .get(5, TimeUnit.SECONDS);
        return cliente;
    }

    protected int statusDoHandshake(Long mesaId, String token) throws Exception {
        return statusDoHandshake(mesaId, token, ORIGEM_PERMITIDA);
    }

    protected int statusDoHandshake(Long mesaId, String token, String origem) throws Exception {
        try {
            clienteWebSocket(token, origem)
                    .buildAsync(enderecoDoChat(mesaId), new WebSocket.Listener() {})
                    .get(5, TimeUnit.SECONDS)
                    .abort();
            return 101;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WebSocketHandshakeException recusa) {
                return recusa.getResponse().statusCode();
            }
            throw e;
        }
    }
}
