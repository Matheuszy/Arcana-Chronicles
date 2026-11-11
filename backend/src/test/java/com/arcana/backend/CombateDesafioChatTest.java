package com.arcana.backend;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.chat.CombateChat;
import com.arcana.backend.chat.DesafioChat;
import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.SalaChat;
import com.arcana.backend.chat.combate.CombateService;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import com.arcana.backend.chat.sala.SalaService;
import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.config.JacksonConfig;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.FichaDeParticipante;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.service.MesaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CombateDesafioChatTest {

    private static final AcessoMesaDto MESTRE = acesso(10L, "Mestre", "MESTRE");
    private static final AcessoMesaDto ARIA = acesso(20L, "Aria", "JOGADOR");
    private static final AcessoMesaDto BROM = acesso(30L, "Brom", "JOGADOR");
    private static final AcessoMesaDto CAEL = acesso(40L, "Cael", "JOGADOR");

    private static final String ROLAGEM_LIVRE = """
            {"type":"ROLAGEM","rollRequest":{"lados":20,"quantidade":1,"modificador":0,"modo":"NORMAL"}}
            """;

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final Deque<Integer> faces = new ArrayDeque<>();
    private MesaChatHandler handler;
    private WebSocketSession sessaoMestre;
    private WebSocketSession sessaoAria;
    private WebSocketSession sessaoBrom;
    private WebSocketSession sessaoCael;
    private MesaService mesaService;

    @BeforeEach
    void setUp() throws Exception {
        RandomGenerator dados = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return faces.removeFirst() - 1;
            }
        };
        Clock relogio = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
        RolagemDadosService rolagens = new RolagemDadosService(dados);
        mesaService = mock(MesaService.class);
        DesafioChat desafioChat = new DesafioChat(new DesafioService(mesaService, rolagens, relogio), objectMapper);
        SalaChat salaChat = new SalaChat(new SalaService(mesaService, relogio), objectMapper);
        CombateChat combateChat = new CombateChat(new CombateService(mesaService, rolagens, relogio), objectMapper);
        handler = new MesaChatHandler(objectMapper, rolagens, new ValidadorImagem(), desafioChat, salaChat,
                combateChat, new GerenciadorMesasAtivas(), mock(TaskScheduler.class), relogio,
                Duration.ofSeconds(60));

        when(mesaService.buscarFichaDoJogadorNaMesa(1L, ARIA.userId())).thenReturn(ficha(ARIA, "Lyra", 14, 0));
        when(mesaService.buscarFichaDoJogadorNaMesa(1L, BROM.userId())).thenReturn(ficha(BROM, "Thorn", 16, 0));
        when(mesaService.buscarFichaDoJogadorNaMesa(1L, CAEL.userId())).thenReturn(ficha(CAEL, "Mira", 12, 0));

        when(mesaService.buscarFichaDeNpcDaMesa(1L, 500L, MESTRE.userId())).thenReturn(fichaDeMonstro());

        sessaoMestre = sessao("m", MESTRE);
        sessaoAria = sessao("a", ARIA);
        sessaoBrom = sessao("b", BROM);
        sessaoCael = sessao("c", CAEL);
        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom, sessaoCael)) {
            handler.afterConnectionEstablished(sessao);
        }
        clearInvocations(sessaoMestre, sessaoAria, sessaoBrom, sessaoCael);
    }

    private static AcessoMesaDto acesso(Long userId, String nome, String papel) {
        return new AcessoMesaDto(1L, userId, nome, papel, StatusMesa.EM_ANDAMENTO);
    }

    private static FichaDeParticipante ficha(AcessoMesaDto jogador, String personagem, int destreza,
                                             int bonusIniciativa) {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, 10);
        }
        valores.put(Atributo.DESTREZA, destreza);
        return new FichaDeParticipante(jogador.userId(), jogador.displayName(), 100L + jogador.userId(), personagem,
                valores, Map.of(), Map.of(), bonusIniciativa);
    }

    private static FichaDeParticipante fichaDeMonstro() {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, 10);
        }
        valores.put(Atributo.DESTREZA, 14);
        return new FichaDeParticipante(MESTRE.userId(), MESTRE.displayName(), 500L, "Goblin", valores, Map.of(),
                Map.of(), 0);
    }

    private WebSocketSession sessao(String id, AcessoMesaDto acesso) {
        Map<String, Object> atributos = new HashMap<>();
        atributos.put(AutenticacaoHandshakeInterceptor.ATRIBUTO_ACESSO, acesso);
        WebSocketSession sessao = mock(WebSocketSession.class);
        when(sessao.getId()).thenReturn(id);
        when(sessao.getAttributes()).thenReturn(atributos);
        when(sessao.isOpen()).thenReturn(true);
        return sessao;
    }

    private void enviar(WebSocketSession origem, String json) throws Exception {
        handler.handleMessage(origem, new TextMessage(json));
    }

    private List<JsonNode> recebidas(WebSocketSession sessao, String tipo) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(sessao, atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues().stream()
                .map(m -> {
                    try {
                        return objectMapper.readTree(m.getPayload());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .filter(no -> tipo.equals(no.get("type").asText()))
                .toList();
    }

    private JsonNode ultimoCombate(WebSocketSession sessao) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "COMBATE");
        JsonNode completos = eventos.get(eventos.size() - 1).get("combates").get("completos");
        return completos.isEmpty() ? NullNode.getInstance() : completos.get(0);
    }

    private String ultimoAviso(WebSocketSession sessao) throws Exception {
        List<JsonNode> avisos = recebidas(sessao, "SISTEMA");
        return avisos.get(avisos.size() - 1).get("content").asText();
    }


    private void rolarIniciativa(WebSocketSession sessao) throws Exception {
        enviar(sessao, """
                {"type":"COMBATE_ROLAR","comandoCombate":{"modo":"NORMAL"}}
                """);
    }

    private void iniciarComAriaNoPrimeiroTurno() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20,30],"monstros":[{"personagemId":500,"quantidade":2}]}}
                """);
        faces.add(18);
        rolarIniciativa(sessaoAria);
        faces.add(5);
        rolarIniciativa(sessaoBrom);
        faces.addAll(List.of(3, 3));
        enviar(sessaoMestre, """
                {"type":"COMBATE_ROLAR_MONSTROS","comandoCombate":{}}
                """);
    }

    private void desafiar(WebSocketSession origem, String alvos) throws Exception {
        enviar(origem, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":13,"descricao":"Bola de fogo","jogadores":%s}}
                """.formatted(alvos));
    }

    private JsonNode ultimoDesafio(WebSocketSession sessao) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "DESAFIO");
        return eventos.get(eventos.size() - 1).get("desafio");
    }

    private JsonNode desafioPorId(WebSocketSession sessao, String desafioId) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "DESAFIO").stream()
                .map(evento -> evento.get("desafio"))
                .filter(desafio -> desafioId.equals(desafio.get("id").asText()))
                .toList();
        return eventos.get(eventos.size() - 1);
    }

    private void confirmar(WebSocketSession criador, String desafioId) throws Exception {
        enviar(criador, """
                {"type":"DESAFIO_CONFIRMAR","comandoDesafio":{"desafioId":"%s"}}
                """.formatted(desafioId));
    }

    private JsonNode alvo(JsonNode desafio, String id) {
        for (JsonNode jogador : desafio.get("jogadores")) {
            if (id.equals(jogador.get("id").asText())) {
                return jogador;
            }
        }
        throw new AssertionError("Alvo ausente: " + id);
    }

    @Test
    void deveAceitarDesafioDeJogadorForaDoCombateSemPrenderAoCombate() throws Exception {
        desafiar(sessaoAria, "[{\"alvoId\":\"30\"}]");

        JsonNode desafio = ultimoDesafio(sessaoBrom);
        assertFalse(desafio.get("doCombate").asBoolean());
        assertFalse(desafio.get("doMestre").asBoolean());
        assertEquals("JOGADOR", alvo(desafio, "30").get("tipo").asText());
        assertTrue(ultimoDesafio(sessaoMestre).get("cd").isNull());
    }

    @Test
    void deveNegarDesafioAntesDaIniciativaCompleta() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20,30]}}
                """);

        desafiar(sessaoAria, "[{\"alvoId\":\"30\"}]");

        assertEquals("O combate só começa depois que todos rolarem a iniciativa.", ultimoAviso(sessaoAria));
    }

    @Test
    void deveNegarDesafioDeQuemNaoEstaNoTurno() throws Exception {
        iniciarComAriaNoPrimeiroTurno();

        desafiar(sessaoBrom, "[{\"alvoId\":\"20\"}]");

        assertEquals("Só é possível desafiar no seu turno.", ultimoAviso(sessaoBrom));
        assertTrue(recebidas(sessaoAria, "DESAFIO").isEmpty());
    }

    @Test
    void deveNegarDesafiarASiMesmoOuAlvoForaDoCombate() throws Exception {
        iniciarComAriaNoPrimeiroTurno();

        desafiar(sessaoAria, "[{\"alvoId\":\"20\"}]");
        assertEquals("Você não pode desafiar a si mesmo.", ultimoAviso(sessaoAria));

        desafiar(sessaoAria, "[{\"alvoId\":\"40\"}]");
        assertEquals("Este alvo não participa do combate.", ultimoAviso(sessaoAria));
    }

    @Test
    void deveDesafiarOsMonstrosAnunciandoAMesaComACdSoParaOsEnvolvidos() throws Exception {
        iniciarComAriaNoPrimeiroTurno();

        desafiar(sessaoAria, "[{\"alvoId\":\"monstro-1\"},{\"alvoId\":\"monstro-2\"}]");

        for (WebSocketSession envolvida : List.of(sessaoAria, sessaoMestre)) {
            JsonNode desafio = ultimoDesafio(envolvida);
            assertEquals(13, desafio.get("cd").asInt());
            assertEquals(20L, desafio.get("criadorId").asLong());
            assertTrue(desafio.get("doCombate").asBoolean());
            assertEquals("MONSTRO", alvo(desafio, "monstro-1").get("tipo").asText());
            assertEquals("Goblin 2", alvo(desafio, "monstro-2").get("nomePersonagem").asText());
            assertEquals("AGUARDANDO_ROLAGEM", alvo(desafio, "monstro-2").get("estado").asText());
        }
        for (WebSocketSession espectadora : List.of(sessaoBrom, sessaoCael)) {
            JsonNode desafio = ultimoDesafio(espectadora);
            assertTrue(desafio.get("cd").isNull());
            assertEquals("Goblin 2", alvo(desafio, "monstro-2").get("nomePersonagem").asText());
        }
    }

    @Test
    void deveRolarUmD20PorMonstroEmUmUnicoComandoDoMestre() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"monstro-1\"},{\"alvoId\":\"monstro-2\"}]");
        String desafioId = ultimoDesafio(sessaoMestre).get("id").asText();

        faces.addAll(List.of(12, 9));
        enviar(sessaoMestre, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"desafioId":"%s","atributo":"destreza","modo":"NORMAL"}}
                """.formatted(desafioId));
        assertEquals("AGUARDANDO_CONFIRMACAO", alvo(ultimoDesafio(sessaoAria), "monstro-1").get("estado").asText());
        confirmar(sessaoMestre, desafioId);
        assertEquals("Apenas quem criou o desafio pode alterá-lo.", ultimoAviso(sessaoMestre));
        confirmar(sessaoAria, desafioId);

        JsonNode desafio = ultimoDesafio(sessaoAria);
        assertEquals("SUCESSO", alvo(desafio, "monstro-1").get("estado").asText());
        assertEquals(14, alvo(desafio, "monstro-1").get("rolagens").get(0).get("total").asInt());
        assertEquals("AGUARDANDO_DECISAO_DO_MESTRE", alvo(desafio, "monstro-2").get("estado").asText());
        assertEquals(11, alvo(desafio, "monstro-2").get("rolagens").get(0).get("total").asInt());
    }

    @Test
    void deveRolarSoOMonstroEscolhidoComVantagem() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"monstro-1\"},{\"alvoId\":\"monstro-2\"}]");
        String desafioId = ultimoDesafio(sessaoMestre).get("id").asText();

        faces.addAll(List.of(4, 15));
        enviar(sessaoMestre, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"desafioId":"%s","alvos":["monstro-2"],"atributo":"destreza","modo":"VANTAGEM"}}
                """.formatted(desafioId));

        JsonNode desafio = ultimoDesafio(sessaoAria);
        assertEquals("VANTAGEM", alvo(desafio, "monstro-2").get("modo").asText());
        assertEquals(17, alvo(desafio, "monstro-2").get("rolagens").get(0).get("total").asInt());
        assertEquals("AGUARDANDO_ROLAGEM", alvo(desafio, "monstro-1").get("estado").asText());
    }

    @Test
    void deveNegarAoJogadorRolarPeloMonstro() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"monstro-1\"}]");

        enviar(sessaoAria, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);

        assertEquals("Você não tem um desafio em andamento.", ultimoAviso(sessaoAria));
    }

    @Test
    void deveDeixarOJogadorDesafiadoRolarForaDoTurnoComAPropriaFicha() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"30\",\"modo\":\"NORMAL\"}]");

        faces.add(12);
        enviar(sessaoBrom, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);
        confirmar(sessaoAria, ultimoDesafio(sessaoAria).get("id").asText());

        JsonNode desafio = ultimoDesafio(sessaoAria);
        assertEquals("SUCESSO", alvo(desafio, "30").get("estado").asText());
        assertEquals(15, alvo(desafio, "30").get("rolagens").get(0).get("total").asInt());
        assertEquals("20", ultimoCombate(sessaoBrom).get("turnoDe").asText());
    }

    @Test
    void deveDeixarSoQuemDesafiouLiberarASegundaTentativa() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"30\"}]");
        String desafioId = ultimoDesafio(sessaoAria).get("id").asText();
        faces.add(2);
        enviar(sessaoBrom, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);
        confirmar(sessaoAria, desafioId);
        assertEquals("AGUARDANDO_DECISAO_DO_MESTRE", alvo(ultimoDesafio(sessaoBrom), "30").get("estado").asText());

        enviar(sessaoMestre, """
                {"type":"DESAFIO_TENTATIVA_EXTRA","comandoDesafio":{"desafioId":"%s","alvoId":"30","modo":"VANTAGEM"}}
                """.formatted(desafioId));
        assertTrue(ultimoAviso(sessaoMestre).startsWith("Apenas "));
        assertEquals("AGUARDANDO_DECISAO_DO_MESTRE", alvo(ultimoDesafio(sessaoAria), "30").get("estado").asText());

        enviar(sessaoAria, """
                {"type":"DESAFIO_TENTATIVA_EXTRA","comandoDesafio":{"desafioId":"%s","alvoId":"30","modo":"VANTAGEM"}}
                """.formatted(desafioId));

        JsonNode liberado = alvo(ultimoDesafio(sessaoBrom), "30");
        assertEquals("AGUARDANDO_TENTATIVA_EXTRA", liberado.get("estado").asText());
        assertEquals("VANTAGEM", liberado.get("modo").asText());
    }

    @Test
    void deveCancelarOsDesafiosDoCombateQuandoOMestreEncerra() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        desafiar(sessaoAria, "[{\"alvoId\":\"30\"},{\"alvoId\":\"monstro-1\"}]");

        enviar(sessaoMestre, "{\"type\":\"COMBATE_ENCERRAR\"}");

        JsonNode desafio = ultimoDesafio(sessaoBrom);
        assertEquals("CANCELADO", alvo(desafio, "30").get("estado").asText());
        assertEquals("CANCELADO", alvo(ultimoDesafio(sessaoMestre), "monstro-1").get("estado").asText());
        enviar(sessaoBrom, """
                {"type":"DESAFIO_ROLAR","comandoDesafio":{"atributo":"destreza"}}
                """);
        assertEquals("Você não tem um desafio em andamento.", ultimoAviso(sessaoBrom));
    }

    @Test
    void deveCancelarSoOsDesafiosDoCombateEncerradoMantendoOsDoOutroCombate() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        String combateDaAria = ultimoCombate(sessaoAria).get("id").asText();
        desafiar(sessaoAria, "[{\"alvoId\":\"30\"}]");
        String desafioDaAria = ultimoDesafio(sessaoAria).get("id").asText();

        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[40],"monstros":[{"personagemId":500,"quantidade":1}]}}
                """);
        String combateDoCael = ultimoCombate(sessaoCael).get("id").asText();
        faces.add(19);
        rolarIniciativa(sessaoCael);
        faces.add(2);
        enviar(sessaoMestre, """
                {"type":"COMBATE_ROLAR_MONSTROS","comandoCombate":{"combateId":"%s"}}
                """.formatted(combateDoCael));
        desafiar(sessaoCael, "[{\"alvoId\":\"monstro-1\"}]");
        String desafioDoCael = ultimoDesafio(sessaoCael).get("id").asText();
        assertTrue(ultimoDesafio(sessaoCael).get("doCombate").asBoolean());

        enviar(sessaoMestre, """
                {"type":"COMBATE_ENCERRAR","comandoCombate":{"combateId":"%s"}}
                """.formatted(combateDaAria));

        assertEquals("CANCELADO", alvo(desafioPorId(sessaoBrom, desafioDaAria), "30").get("estado").asText());
        JsonNode doCael = desafioPorId(sessaoCael, desafioDoCael);
        assertTrue(doCael.get("ativo").asBoolean());
        assertEquals("AGUARDANDO_ROLAGEM", alvo(doCael, "monstro-1").get("estado").asText());
    }

    @Test
    void deveNegarAoJogadorDesafiarUmMonstroOculto() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20,30],"monstros":[{"personagemId":500,"quantidade":1,"oculto":true}]}}
                """);
        faces.add(18);
        rolarIniciativa(sessaoAria);
        faces.add(5);
        rolarIniciativa(sessaoBrom);
        faces.add(3);
        enviar(sessaoMestre, """
                {"type":"COMBATE_ROLAR_MONSTROS","comandoCombate":{}}
                """);

        desafiar(sessaoAria, "[{\"alvoId\":\"monstro-1\"}]");

        assertEquals("Este alvo não participa do combate.", ultimoAviso(sessaoAria));
        assertTrue(recebidas(sessaoMestre, "DESAFIO").isEmpty());
    }

    private void ajudar(WebSocketSession autor, String desafioId, int alvo, String tipo) throws Exception {
        enviar(autor, """
                {"type":"DESAFIO_AJUDAR","comandoDesafio":{"desafioId":"%s","jogadorId":%d,"tipoAjuda":"%s"}}
                """.formatted(desafioId, alvo, tipo));
    }

    private JsonNode entrada(JsonNode combate, String id) {
        for (JsonNode participante : combate.get("participantes")) {
            if (id.equals(participante.get("id").asText())) {
                return participante;
            }
        }
        throw new AssertionError("Participante ausente: " + id);
    }

    @Test
    void deveGastarAReacaoDeQuemAjudaDentroDoCombateEDevolverNoInicioDoTurno() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":40,"modo":"NORMAL"},{"jogadorId":20,"modo":"NORMAL"}]}}
                """);
        String desafioId = ultimoDesafio(sessaoMestre).get("id").asText();

        ajudar(sessaoBrom, desafioId, 40, "VANTAGEM");
        assertEquals(1, alvo(ultimoDesafio(sessaoCael), "40").get("ajudas").size());
        assertTrue(entrada(ultimoCombate(sessaoAria), "30").get("reacaoUsada").asBoolean());
        assertTrue(entrada(ultimoCombate(sessaoAria), "30").get("reacaoAberta").isNull());

        ajudar(sessaoBrom, desafioId, 20, "DESVANTAGEM");
        assertEquals("Thorn já usou a reação nesta rodada. Ela volta no início do próximo turno.",
                ultimoAviso(sessaoBrom));
        assertEquals(0, alvo(ultimoDesafio(sessaoMestre), "20").get("ajudas").size());

        enviar(sessaoAria, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertFalse(entrada(ultimoCombate(sessaoBrom), "30").get("reacaoUsada").asBoolean());
        ajudar(sessaoBrom, desafioId, 20, "VANTAGEM");
        assertEquals("A reação é usada fora do próprio turno.", ultimoAviso(sessaoBrom));

        enviar(sessaoBrom, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        ajudar(sessaoBrom, desafioId, 20, "VANTAGEM");
        assertEquals(1, alvo(ultimoDesafio(sessaoMestre), "20").get("ajudas").size());
    }

    @Test
    void deveNegarAjudaDeQuemEstaNoProprioTurnoDoCombate() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":40,"modo":"NORMAL"}]}}
                """);
        String desafioId = ultimoDesafio(sessaoMestre).get("id").asText();

        ajudar(sessaoAria, desafioId, 40, "VANTAGEM");

        assertEquals("A reação é usada fora do próprio turno.", ultimoAviso(sessaoAria));
        assertEquals(0, alvo(ultimoDesafio(sessaoMestre), "40").get("ajudas").size());
        assertFalse(entrada(ultimoCombate(sessaoAria), "20").get("reacaoUsada").asBoolean());
    }

    @Test
    void deveDeixarAjudarSemGastarReacaoForaDoCombateEPeloMestre() throws Exception {
        iniciarComAriaNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":30,"modo":"NORMAL"},{"jogadorId":20,"modo":"NORMAL"}]}}
                """);
        String desafioId = ultimoDesafio(sessaoMestre).get("id").asText();
        clearInvocations(sessaoAria);

        ajudar(sessaoCael, desafioId, 30, "VANTAGEM");
        ajudar(sessaoCael, desafioId, 20, "DESVANTAGEM");
        ajudar(sessaoMestre, desafioId, 30, "ANULAR_DESVANTAGEM");

        JsonNode desafio = ultimoDesafio(sessaoMestre);
        assertEquals(2, alvo(desafio, "30").get("ajudas").size());
        assertEquals(1, alvo(desafio, "20").get("ajudas").size());
        assertTrue(recebidas(sessaoAria, "COMBATE").isEmpty());
        assertTrue(recebidas(sessaoCael, "SISTEMA").isEmpty());
    }

    @Test
    void deveManterODesafioDoMestreForaDoCombateComoAntes() throws Exception {
        enviar(sessaoMestre, """
                {"type":"DESAFIO_CRIAR","comandoDesafio":{"cd":10,"jogadores":[{"jogadorId":30,"modo":"NORMAL"}]}}
                """);

        JsonNode desafio = ultimoDesafio(sessaoBrom);
        assertFalse(desafio.get("doCombate").asBoolean());
        assertEquals("JOGADOR", alvo(desafio, "30").get("tipo").asText());
        assertEquals(10L, desafio.get("criadorId").asLong());
    }
}
