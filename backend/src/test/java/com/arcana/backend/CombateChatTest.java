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

class CombateChatTest {

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

        when(mesaService.buscarFichaDoJogadorNaMesa(1L, ARIA.userId())).thenReturn(ficha(ARIA, "Lyra", 14, 3));
        when(mesaService.buscarFichaDoJogadorNaMesa(1L, BROM.userId())).thenReturn(ficha(BROM, "Thorn", 16, 2));

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
                                             int bonusProficiencia) {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, 10);
        }
        valores.put(Atributo.DESTREZA, destreza);
        return new FichaDeParticipante(jogador.userId(), jogador.displayName(), 100L + jogador.userId(), personagem,
                valores, Map.of(), Map.of(), bonusProficiencia);
    }

    private static FichaDeParticipante fichaDeMonstro() {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, 10);
        }
        valores.put(Atributo.DESTREZA, 14);
        return new FichaDeParticipante(MESTRE.userId(), MESTRE.displayName(), 500L, "Goblin", valores, Map.of(),
                Map.of(), 2);
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
        return eventos.get(eventos.size() - 1).get("combate");
    }

    private String ultimoAviso(WebSocketSession sessao) throws Exception {
        List<JsonNode> avisos = recebidas(sessao, "SISTEMA");
        return avisos.get(avisos.size() - 1).get("content").asText();
    }

    private void iniciarCombateComAriaEBrom() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20,30]}}
                """);
    }

    private void rolarIniciativa(WebSocketSession sessao, String modo, boolean somarProficiencia) throws Exception {
        enviar(sessao, """
                {"type":"COMBATE_ROLAR","comandoCombate":{"modo":"%s","somarProficiencia":%s}}
                """.formatted(modo, somarProficiencia));
    }

    private void iniciarComBromNoPrimeiroTurno() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(10, 12));
        rolarIniciativa(sessaoAria, "NORMAL", true);
        rolarIniciativa(sessaoBrom, "NORMAL", false);
    }

    @Test
    void deveNegarInicioDeCombateParaJogadorSemNotificarNinguem() throws Exception {
        enviar(sessaoAria, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[30]}}
                """);

        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoAria));
        assertTrue(recebidas(sessaoMestre, "COMBATE").isEmpty());
        assertTrue(recebidas(sessaoBrom, "COMBATE").isEmpty());
    }

    @Test
    void deveIniciarCombateAvisandoTodosEAguardandoIniciativaDosEscolhidos() throws Exception {
        iniciarCombateComAriaEBrom();

        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom, sessaoCael)) {
            JsonNode combate = ultimoCombate(sessao);
            assertEquals("INICIATIVA", combate.get("fase").asText());
            assertEquals(2, combate.get("participantes").size());
            assertTrue(combate.get("participantes").get(0).get("aguardandoRolagem").asBoolean());
        }
        assertEquals("O mestre iniciou o combate. Quem foi escolhido deve rolar a iniciativa.",
                ultimoAviso(sessaoCael));
    }

    @Test
    void deveNegarIniciativaParaQuemNaoFoiEscolhido() throws Exception {
        iniciarCombateComAriaEBrom();

        rolarIniciativa(sessaoCael, "NORMAL", false);

        assertEquals("Este jogador não participa do combate.", ultimoAviso(sessaoCael));
    }

    @Test
    void deveSomarDestrezaEProficienciaSoQuandoOJogadorEscolher() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(10, 12));

        rolarIniciativa(sessaoAria, "NORMAL", true);
        rolarIniciativa(sessaoBrom, "NORMAL", false);

        JsonNode participantes = ultimoCombate(sessaoMestre).get("participantes");
        JsonNode aria = participantes.get(1).get("iniciativa");
        JsonNode brom = participantes.get(0).get("iniciativa");
        assertEquals(15, aria.get("total").asInt());
        assertEquals(2, aria.get("modificadorDestreza").asInt());
        assertEquals(3, aria.get("bonusProficiencia").asInt());
        assertEquals(15, brom.get("total").asInt());
        assertEquals(0, brom.get("bonusProficiencia").asInt());
    }

    @Test
    void deveRolarComVantagemEManterOMaiorDado() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(4, 15));

        rolarIniciativa(sessaoAria, "VANTAGEM", false);

        JsonNode aria = ultimoCombate(sessaoAria).get("participantes").get(0).get("iniciativa");
        assertEquals(15, aria.get("mantido").asInt());
        assertEquals(17, aria.get("total").asInt());
        assertEquals("VANTAGEM", aria.get("modo").asText());
    }

    @Test
    void deveMostrarAListaOrdenadaSoQuandoTodosRolaremDesempatandoPelaDestreza() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.add(10);
        rolarIniciativa(sessaoAria, "NORMAL", true);

        assertEquals("INICIATIVA", ultimoCombate(sessaoBrom).get("fase").asText());
        assertEquals("O mestre iniciou o combate. Quem foi escolhido deve rolar a iniciativa.",
                ultimoAviso(sessaoBrom));

        faces.add(12);
        rolarIniciativa(sessaoBrom, "NORMAL", false);

        JsonNode combate = ultimoCombate(sessaoBrom);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(30L, combate.get("turnoDe").asLong());
        assertEquals("Thorn", combate.get("participantes").get(0).get("nomePersonagem").asText());
        assertEquals("Lyra", combate.get("participantes").get(1).get("nomePersonagem").asText());
        List<JsonNode> avisos = recebidas(sessaoCael, "SISTEMA");
        assertEquals("Ordem de iniciativa:\n1. Thorn (Brom): 15\n2. Lyra (Aria): 15",
                avisos.get(avisos.size() - 2).get("content").asText());
        assertEquals("Rodada 1: turno de Thorn (Brom).", avisos.get(avisos.size() - 1).get("content").asText());
    }

    @Test
    void deveNegarSegundaRolagemAteOMestreLiberar() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        rolarIniciativa(sessaoAria, "NORMAL", false);

        assertEquals("Você já rolou a iniciativa. Peça ao mestre para liberar uma nova rolagem.",
                ultimoAviso(sessaoAria));
    }

    @Test
    void deveReordenarAListaQuandoOMestreLiberaENovaRolagemSuperaOutroJogador() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        enviar(sessaoMestre, """
                {"type":"COMBATE_LIBERAR_REROLL","comandoCombate":{"entradas":["20"]}}
                """);

        assertTrue(ultimoCombate(sessaoAria).get("participantes").get(1).get("rerolagemLiberada").asBoolean());
        assertEquals("O mestre liberou uma nova rolagem de iniciativa para Lyra.", ultimoAviso(sessaoAria));

        faces.add(20);
        rolarIniciativa(sessaoAria, "NORMAL", true);

        JsonNode combate = ultimoCombate(sessaoMestre);
        assertEquals("Lyra", combate.get("participantes").get(0).get("nomePersonagem").asText());
        assertEquals(25, combate.get("participantes").get(0).get("iniciativa").get("total").asInt());
        assertEquals(30L, combate.get("turnoDe").asLong());
        assertFalse(combate.get("participantes").get(0).get("rerolagemLiberada").asBoolean());
    }

    @Test
    void deveNegarLiberacaoDeRerolagemParaJogadorOuAntesDaListaPronta() throws Exception {
        iniciarCombateComAriaEBrom();

        enviar(sessaoAria, """
                {"type":"COMBATE_LIBERAR_REROLL","comandoCombate":{"entradas":["20"]}}
                """);
        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoAria));

        enviar(sessaoMestre, """
                {"type":"COMBATE_LIBERAR_REROLL","comandoCombate":{"entradas":["20"]}}
                """);
        assertEquals("A rolagem só pode ser liberada depois que todos rolarem a iniciativa.",
                ultimoAviso(sessaoMestre));
    }

    @Test
    void devePassarOTurnoSoParaQuemEstaNaVezOuParaOMestreContandoRodadas() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        enviar(sessaoAria, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals("Só quem está no turno, ou o mestre, pode passar o turno.", ultimoAviso(sessaoAria));
        assertEquals(30L, ultimoCombate(sessaoAria).get("turnoDe").asLong());

        enviar(sessaoBrom, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals(20L, ultimoCombate(sessaoAria).get("turnoDe").asLong());
        assertEquals(1, ultimoCombate(sessaoAria).get("rodada").asInt());

        enviar(sessaoMestre, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals(30L, ultimoCombate(sessaoAria).get("turnoDe").asLong());
        assertEquals(2, ultimoCombate(sessaoAria).get("rodada").asInt());
        assertEquals("Rodada 2: turno de Thorn (Brom).", ultimoAviso(sessaoAria));
    }

    @Test
    void deveBloquearRolagemLivreForaDoTurnoIncluindoOMestre() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        faces.add(9);

        enviar(sessaoBrom, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());

        enviar(sessaoAria, ROLAGEM_LIVRE);
        enviar(sessaoMestre, ROLAGEM_LIVRE);
        enviar(sessaoCael, ROLAGEM_LIVRE);

        String bloqueio = "Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.";
        assertEquals(bloqueio, ultimoAviso(sessaoAria));
        assertEquals(bloqueio, ultimoAviso(sessaoMestre));
        assertEquals(bloqueio, ultimoAviso(sessaoCael));
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
    }

    @Test
    void deveBloquearRolagemLivreAteTodosRolaremAIniciativa() throws Exception {
        iniciarCombateComAriaEBrom();

        enviar(sessaoAria, ROLAGEM_LIVRE);

        assertEquals("Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.",
                ultimoAviso(sessaoAria));
        assertTrue(recebidas(sessaoMestre, "ROLAGEM").isEmpty());
    }

    @Test
    void deveManterOutrosCanaisLivresDuranteOCombate() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        enviar(sessaoAria, """
                {"type":"TEXTO","canal":"CONVERSA","content":"Cuidado com a esquerda!"}
                """);

        assertEquals(1, recebidas(sessaoMestre, "TEXTO").size());
    }

    @Test
    void deveEncerrarCombateSoPeloMestreELiberarOsDados() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        enviar(sessaoBrom, "{\"type\":\"COMBATE_ENCERRAR\"}");
        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoBrom));

        enviar(sessaoMestre, "{\"type\":\"COMBATE_ENCERRAR\"}");

        assertTrue(ultimoCombate(sessaoAria).isNull());
        assertEquals("O combate terminou. Os chats voltam a ser livres.", ultimoAviso(sessaoAria));

        faces.add(7);
        enviar(sessaoAria, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
    }

    @Test
    void deveRecusarSegundoCombateEnquantoUmEstiverAtivo() throws Exception {
        iniciarCombateComAriaEBrom();

        iniciarCombateComAriaEBrom();

        assertEquals("Já existe um combate em andamento.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveExigirAoMenosUmJogadorParaIniciarOCombate() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[]}}
                """);

        assertEquals("Selecione ao menos um jogador para o combate.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveEnviarOCombateAtualAoNovoConectado() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        WebSocketSession retornada = sessao("r", CAEL);

        handler.afterConnectionEstablished(retornada);

        JsonNode combate = ultimoCombate(retornada);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(30L, combate.get("turnoDe").asLong());
    }

    private void iniciarComAriaETresGoblins() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":3}]}}
                """);
    }

    private void rolarMonstros(String comando) throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_ROLAR_MONSTROS","comandoCombate":%s}
                """.formatted(comando));
    }

    private JsonNode participante(JsonNode combate, String id) {
        for (JsonNode participante : combate.get("participantes")) {
            if (id.equals(participante.get("id").asText())) {
                return participante;
            }
        }
        throw new AssertionError("Participante ausente: " + id);
    }

    @Test
    void deveCriarUmaEntradaPorMonstroDoGrupoSemDuplicarAFicha() throws Exception {
        iniciarComAriaETresGoblins();

        JsonNode combate = ultimoCombate(sessaoAria);
        assertEquals(4, combate.get("participantes").size());
        assertEquals("Lyra", participante(combate, "20").get("nomePersonagem").asText());
        for (int numero = 1; numero <= 3; numero++) {
            JsonNode monstro = participante(combate, "monstro-" + numero);
            assertEquals("Goblin " + numero, monstro.get("nomePersonagem").asText());
            assertEquals("MONSTRO", monstro.get("tipo").asText());
            assertEquals(500L, monstro.get("personagemId").asLong());
            assertEquals("Goblin", monstro.get("grupo").asText());
            assertEquals(10L, monstro.get("controladorId").asLong());
        }
        verify(mesaService, times(1)).buscarFichaDeNpcDaMesa(1L, 500L, MESTRE.userId());
    }

    @Test
    void deveUsarONomeEscolhidoPeloMestreNoLugarDoNomeDaFicha() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":2,"nome":" Orc da Ponte "}]}}
                """);

        JsonNode combate = ultimoCombate(sessaoMestre);
        assertEquals("Orc da Ponte 1", participante(combate, "monstro-1").get("nomePersonagem").asText());
        assertEquals("Orc da Ponte 2", participante(combate, "monstro-2").get("nomePersonagem").asText());
        assertEquals("Orc da Ponte", participante(combate, "monstro-1").get("grupo").asText());
    }

    @Test
    void deveRecusarNomeDeMonstroMuitoLongo() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":1,"nome":"%s"}]}}
                """.formatted("x".repeat(61)));

        assertEquals("O nome do monstro deve ter até 60 caracteres.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveNomearMonstroSemNumeroQuandoOGrupoTemUmSo() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":1}]}}
                """);

        assertEquals("Goblin", participante(ultimoCombate(sessaoMestre), "monstro-1").get("nomePersonagem").asText());
    }

    @Test
    void deveRolarUmD20PorMonstroComUmUnicoComandoEFecharAOrdemComOsJogadores() throws Exception {
        iniciarComAriaETresGoblins();
        faces.add(10);
        rolarIniciativa(sessaoAria, "NORMAL", false);
        assertEquals("INICIATIVA", ultimoCombate(sessaoAria).get("fase").asText());

        faces.addAll(List.of(3, 18, 11));
        rolarMonstros("{\"modo\":\"NORMAL\",\"somarProficiencia\":false}");

        JsonNode combate = ultimoCombate(sessaoAria);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(5, participante(combate, "monstro-1").get("iniciativa").get("total").asInt());
        assertEquals(20, participante(combate, "monstro-2").get("iniciativa").get("total").asInt());
        assertEquals(13, participante(combate, "monstro-3").get("iniciativa").get("total").asInt());
        assertEquals("monstro-2", combate.get("turnoDe").asText());
        assertEquals(10L, combate.get("controladorDoTurno").asLong());
        List<JsonNode> avisos = recebidas(sessaoCael, "SISTEMA");
        assertEquals("Ordem de iniciativa:\n1. Goblin 2 (Mestre): 20\n2. Goblin 3 (Mestre): 13\n3. Lyra (Aria): 12\n4. Goblin 1 (Mestre): 5",
                avisos.get(avisos.size() - 2).get("content").asText());
        assertEquals("Rodada 1: turno de Goblin 2 (Mestre).", avisos.get(avisos.size() - 1).get("content").asText());
    }

    @Test
    void deveRolarSoOsMonstrosEscolhidosComVantagemEProficiencia() throws Exception {
        iniciarComAriaETresGoblins();

        faces.addAll(List.of(4, 15));
        rolarMonstros("{\"entradas\":[\"monstro-2\"],\"modo\":\"VANTAGEM\",\"somarProficiencia\":true}");

        JsonNode combate = ultimoCombate(sessaoMestre);
        JsonNode iniciativa = participante(combate, "monstro-2").get("iniciativa");
        assertEquals(19, iniciativa.get("total").asInt());
        assertEquals(15, iniciativa.get("mantido").asInt());
        assertEquals(2, iniciativa.get("bonusProficiencia").asInt());
        assertTrue(participante(combate, "monstro-1").get("iniciativa").isNull());
        assertTrue(participante(combate, "monstro-3").get("iniciativa").isNull());
    }

    @Test
    void deveNegarAoJogadorRolarAIniciativaDosMonstros() throws Exception {
        iniciarComAriaETresGoblins();

        enviar(sessaoAria, """
                {"type":"COMBATE_ROLAR_MONSTROS","comandoCombate":{"modo":"NORMAL"}}
                """);

        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoAria));
    }

    @Test
    void deveNegarRolarComoMonstroAEntradaDeUmJogador() throws Exception {
        iniciarComAriaETresGoblins();

        rolarMonstros("{\"entradas\":[\"20\"]}");

        assertEquals("O mestre só rola a iniciativa dos monstros, NPCs e chefes.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveNegarSegundaRolagemDoMonstroAteOMestreLiberar() throws Exception {
        iniciarComAriaETresGoblins();
        faces.addAll(List.of(3, 18, 11));
        rolarMonstros("{}");

        rolarMonstros("{\"entradas\":[\"monstro-2\"]}");
        assertEquals("Você já rolou a iniciativa. Peça ao mestre para liberar uma nova rolagem.",
                ultimoAviso(sessaoMestre));

        faces.add(20);
        rolarIniciativa(sessaoAria, "NORMAL", false);
        enviar(sessaoMestre, """
                {"type":"COMBATE_LIBERAR_REROLL","comandoCombate":{"entradas":["monstro-2"]}}
                """);
        faces.add(1);
        rolarMonstros("{\"entradas\":[\"monstro-2\"]}");

        assertEquals(3, participante(ultimoCombate(sessaoMestre), "monstro-2").get("iniciativa").get("total").asInt());
    }

    @Test
    void deveRecusarRolagemDosMonstrosQuandoNenhumAguardaRolagem() throws Exception {
        iniciarComAriaETresGoblins();
        faces.addAll(List.of(3, 18, 11));
        rolarMonstros("{}");

        rolarMonstros("{}");

        assertEquals("Nenhum monstro aguarda rolagem de iniciativa.", ultimoAviso(sessaoMestre));
    }

    @Test
    void devePermitirAoMestreRolarLivrementeSoNoTurnoDeUmMonstro() throws Exception {
        iniciarComAriaETresGoblins();
        faces.add(10);
        rolarIniciativa(sessaoAria, "NORMAL", false);
        faces.addAll(List.of(18, 3, 3));
        rolarMonstros("{}");
        faces.add(9);

        enviar(sessaoMestre, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());

        enviar(sessaoAria, ROLAGEM_LIVRE);
        assertEquals("Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.",
                ultimoAviso(sessaoAria));
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
    }

    @Test
    void devePassarOTurnoDoMonstroSoParaOMestre() throws Exception {
        iniciarComAriaETresGoblins();
        faces.add(10);
        rolarIniciativa(sessaoAria, "NORMAL", false);
        faces.addAll(List.of(18, 3, 3));
        rolarMonstros("{}");

        enviar(sessaoAria, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals("Só quem está no turno, ou o mestre, pode passar o turno.", ultimoAviso(sessaoAria));
        assertEquals("monstro-1", ultimoCombate(sessaoAria).get("turnoDe").asText());

        enviar(sessaoMestre, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals("20", ultimoCombate(sessaoAria).get("turnoDe").asText());
        assertEquals(20L, ultimoCombate(sessaoAria).get("controladorDoTurno").asLong());
        faces.add(8);
        enviar(sessaoAria, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
    }

    @Test
    void deveNegarGrupoDeFichaRepetidaOuComQuantidadeInvalida() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":1},{"personagemId":500,"quantidade":2}]}}
                """);
        assertEquals("Cada ficha só pode aparecer uma vez. Use a quantidade para replicar.",
                ultimoAviso(sessaoMestre));

        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":21}]}}
                """);
        assertEquals("A quantidade de monstros deve ficar entre 1 e 20.", ultimoAviso(sessaoMestre));

        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":0}]}}
                """);
        assertEquals("A quantidade de monstros deve ficar entre 1 e 20.", ultimoAviso(sessaoMestre));
        assertTrue(recebidas(sessaoAria, "COMBATE").isEmpty());
    }

    @Test
    void deveNegarAoJogadorIniciarCombateComMonstros() throws Exception {
        enviar(sessaoAria, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":2}]}}
                """);

        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoAria));
        verify(mesaService, never()).buscarFichaDeNpcDaMesa(anyLong(), anyLong(), anyLong());
    }
}
