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
                Map.of(), 1);
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

    private JsonNode ultimosResumos(WebSocketSession sessao) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "COMBATE");
        return eventos.get(eventos.size() - 1).get("combates").get("resumos");
    }

    private JsonNode combatesDoMestre() throws Exception {
        List<JsonNode> eventos = recebidas(sessaoMestre, "COMBATE");
        return eventos.get(eventos.size() - 1).get("combates").get("completos");
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

    private void iniciarCombateComAriaEBrom() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20,30]}}
                """);
    }

    private void rolarIniciativa(WebSocketSession sessao, String modo) throws Exception {
        enviar(sessao, """
                {"type":"COMBATE_ROLAR","comandoCombate":{"modo":"%s"}}
                """.formatted(modo));
    }

    private void iniciarComBromNoPrimeiroTurno() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(10, 12));
        rolarIniciativa(sessaoAria, "NORMAL");
        rolarIniciativa(sessaoBrom, "NORMAL");
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
    void deveIniciarCombateMostrandoOPainelAosEnvolvidosEOResumoAosDemaisSemAvisoNoChat() throws Exception {
        iniciarCombateComAriaEBrom();

        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom)) {
            JsonNode combate = ultimoCombate(sessao);
            assertEquals("Combate 1", combate.get("nome").asText());
            assertEquals("INICIATIVA", combate.get("fase").asText());
            assertEquals(2, combate.get("participantes").size());
            assertTrue(combate.get("participantes").get(0).get("aguardandoRolagem").asBoolean());
            assertTrue(recebidas(sessao, "SISTEMA").isEmpty());
        }
        assertTrue(ultimoCombate(sessaoCael).isNull());
        JsonNode resumo = ultimosResumos(sessaoCael).get(0);
        assertEquals("Combate 1", resumo.get("nome").asText());
        assertEquals(2, resumo.get("jogadores").size());
        assertFalse(resumo.has("participantes"));
        assertTrue(recebidas(sessaoCael, "SISTEMA").isEmpty());
    }

    @Test
    void deveNegarIniciativaParaQuemNaoFoiEscolhido() throws Exception {
        iniciarCombateComAriaEBrom();

        rolarIniciativa(sessaoCael, "NORMAL");

        assertEquals("Este jogador não participa do combate.", ultimoAviso(sessaoCael));
    }

    @Test
    void deveSomarDestrezaEBonusDeIniciativaDaFichaSemProficiencia() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(10, 12));

        rolarIniciativa(sessaoAria, "NORMAL");
        rolarIniciativa(sessaoBrom, "NORMAL");

        JsonNode participantes = ultimoCombate(sessaoMestre).get("participantes");
        JsonNode aria = participantes.get(1).get("iniciativa");
        JsonNode brom = participantes.get(0).get("iniciativa");
        assertEquals(15, aria.get("total").asInt());
        assertEquals(2, aria.get("modificadorDestreza").asInt());
        assertEquals(3, aria.get("bonusIniciativa").asInt());
        assertFalse(aria.has("bonusProficiencia"));
        assertEquals(15, brom.get("total").asInt());
        assertEquals(0, brom.get("bonusIniciativa").asInt());
    }

    @Test
    void deveIgnorarPedidoDeSomarProficienciaNaIniciativa() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.add(12);

        enviar(sessaoBrom, """
                {"type":"COMBATE_ROLAR","comandoCombate":{"modo":"NORMAL","somarProficiencia":true}}
                """);

        JsonNode brom = participante(ultimoCombate(sessaoMestre), "30").get("iniciativa");
        assertEquals(15, brom.get("total").asInt());
    }

    @Test
    void deveRolarComVantagemEManterOMaiorDado() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.addAll(List.of(4, 15));

        rolarIniciativa(sessaoAria, "VANTAGEM");

        JsonNode aria = ultimoCombate(sessaoAria).get("participantes").get(0).get("iniciativa");
        assertEquals(15, aria.get("mantido").asInt());
        assertEquals(20, aria.get("total").asInt());
        assertEquals("VANTAGEM", aria.get("modo").asText());
    }

    @Test
    void deveMostrarAListaOrdenadaSoQuandoTodosRolaremDesempatandoPelaDestreza() throws Exception {
        iniciarCombateComAriaEBrom();
        faces.add(10);
        rolarIniciativa(sessaoAria, "NORMAL");

        assertEquals("INICIATIVA", ultimoCombate(sessaoBrom).get("fase").asText());

        faces.add(12);
        rolarIniciativa(sessaoBrom, "NORMAL");

        JsonNode combate = ultimoCombate(sessaoBrom);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(30L, combate.get("turnoDe").asLong());
        assertEquals("Thorn", combate.get("participantes").get(0).get("nomePersonagem").asText());
        assertEquals("Lyra", combate.get("participantes").get(1).get("nomePersonagem").asText());
        assertTrue(recebidas(sessaoCael, "SISTEMA").isEmpty());
    }

    @Test
    void deveNegarSegundaRolagemAteOMestreLiberar() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        rolarIniciativa(sessaoAria, "NORMAL");

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
        assertTrue(recebidas(sessaoAria, "SISTEMA").isEmpty());

        faces.add(20);
        rolarIniciativa(sessaoAria, "NORMAL");

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
        assertEquals(1, recebidas(sessaoAria, "SISTEMA").size());
    }

    @Test
    void deveBloquearRolagemLivreForaDoTurnoSoParaQuemEstaNoCombate() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        faces.addAll(List.of(9, 4, 6));

        enviar(sessaoBrom, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());

        enviar(sessaoAria, ROLAGEM_LIVRE);
        assertEquals("Durante o combate você só pode rolar dados no seu turno ou quando for desafiado.",
                ultimoAviso(sessaoAria));
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());

        enviar(sessaoCael, ROLAGEM_LIVRE);
        enviar(sessaoMestre, ROLAGEM_LIVRE);
        assertEquals(3, recebidas(sessaoMestre, "ROLAGEM").size());
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
        assertTrue(recebidas(sessaoAria, "SISTEMA").isEmpty());

        faces.add(7);
        enviar(sessaoAria, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());
    }

    @Test
    void deveRecusarCombateComJogadorQueJaEstaEmOutroCombate() throws Exception {
        iniciarCombateComAriaEBrom();

        iniciarCombateComAriaEBrom();

        assertEquals("Lyra já está em outro combate.", ultimoAviso(sessaoMestre));
        assertEquals(1, combatesDoMestre().size());
    }

    @Test
    void deveRodarDoisCombatesAoMesmoTempoComNomeEscolhidoETurnosIndependentes() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"nome":" Emboscada na ponte ","jogadores":[40]}}
                """);

        JsonNode combates = combatesDoMestre();
        assertEquals(2, combates.size());
        assertEquals("Emboscada na ponte", combates.get(1).get("nome").asText());
        assertEquals("Emboscada na ponte", ultimoCombate(sessaoCael).get("nome").asText());
        assertEquals("Combate 1", ultimosResumos(sessaoCael).get(0).get("nome").asText());
        assertEquals("Emboscada na ponte", ultimosResumos(sessaoAria).get(0).get("nome").asText());

        faces.add(15);
        rolarIniciativa(sessaoCael, "NORMAL");
        assertEquals("EM_ANDAMENTO", ultimoCombate(sessaoCael).get("fase").asText());
        assertEquals(30L, ultimoCombate(sessaoBrom).get("turnoDe").asLong());

        faces.add(8);
        enviar(sessaoCael, ROLAGEM_LIVRE);
        assertEquals(1, recebidas(sessaoMestre, "ROLAGEM").size());

        enviar(sessaoCael, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals(2, ultimoCombate(sessaoCael).get("rodada").asInt());
        assertEquals(1, ultimoCombate(sessaoBrom).get("rodada").asInt());
    }

    @Test
    void deveExigirQueOMestreEscolhaOCombateQuandoHaMaisDeUm() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[40]}}
                """);

        enviar(sessaoMestre, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals("Escolha o combate.", ultimoAviso(sessaoMestre));

        String primeiro = combatesDoMestre().get(0).get("id").asText();
        enviar(sessaoMestre, """
                {"type":"COMBATE_PASSAR_TURNO","comandoCombate":{"combateId":"%s"}}
                """.formatted(primeiro));
        assertEquals(20L, ultimoCombate(sessaoAria).get("turnoDe").asLong());
    }

    @Test
    void deveEncerrarUmCombateSemAfetarOOutro() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[40]}}
                """);
        String primeiro = combatesDoMestre().get(0).get("id").asText();

        enviar(sessaoMestre, """
                {"type":"COMBATE_ENCERRAR","comandoCombate":{"combateId":"%s"}}
                """.formatted(primeiro));

        assertEquals(1, combatesDoMestre().size());
        assertEquals("Combate 2", combatesDoMestre().get(0).get("nome").asText());
        assertTrue(ultimoCombate(sessaoAria).isNull());
        assertEquals("Combate 2", ultimoCombate(sessaoCael).get("nome").asText());
    }

    @Test
    void deveRecusarNomeDeCombateLongoDemais() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"nome":"%s","jogadores":[20]}}
                """.formatted("x".repeat(61)));

        assertEquals("O nome do combate deve ter até 60 caracteres.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveExigirAoMenosUmJogadorParaIniciarOCombate() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[]}}
                """);

        assertEquals("Selecione ao menos um jogador para o combate.", ultimoAviso(sessaoMestre));
    }

    @Test
    void deveEnviarOCombateAtualAoNovoConectadoConformeOQueEleVe() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        WebSocketSession aria = sessao("r", ARIA);
        WebSocketSession cael = sessao("s", CAEL);

        handler.afterConnectionEstablished(aria);
        handler.afterConnectionEstablished(cael);

        JsonNode combate = ultimoCombate(aria);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(30L, combate.get("turnoDe").asLong());
        assertTrue(ultimoCombate(cael).isNull());
        assertEquals("EM_ANDAMENTO", ultimosResumos(cael).get(0).get("fase").asText());
    }

    private void iniciarComAriaETresGoblins() throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_INICIAR","comandoCombate":{"jogadores":[20],"monstros":[{"personagemId":500,"quantidade":3}]}}
                """);
    }

    private JsonNode ultimaEntrada(WebSocketSession sessao) throws Exception {
        List<JsonNode> eventos = recebidas(sessao, "COMBATE_ENTRADA");
        return eventos.get(eventos.size() - 1).get("entradaNoCombate");
    }

    private void adicionarAoCombate(String comando) throws Exception {
        enviar(sessaoMestre, """
                {"type":"COMBATE_ADICIONAR","comandoCombate":%s}
                """.formatted(comando));
    }

    @Test
    void deveAdicionarJogadorAoCombateEmAndamentoAnunciandoATodosMenosAQuemEntrou() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        adicionarAoCombate("{\"jogadores\":[40]}");

        for (WebSocketSession sessao : List.of(sessaoMestre, sessaoAria, sessaoBrom)) {
            JsonNode entrada = ultimaEntrada(sessao);
            assertEquals("Combate 1", entrada.get("combateNome").asText());
            assertEquals("Mira", entrada.get("nomes").get(0).asText());
        }
        assertTrue(recebidas(sessaoCael, "COMBATE_ENTRADA").isEmpty());

        JsonNode mira = participante(ultimoCombate(sessaoCael), "40");
        assertTrue(mira.get("aguardandoRolagem").asBoolean());
        assertTrue(mira.get("aguardandoFimDoTurno").asBoolean());
        assertEquals("EM_ANDAMENTO", ultimoCombate(sessaoCael).get("fase").asText());
        assertEquals(30L, ultimoCombate(sessaoCael).get("turnoDe").asLong());
    }

    @Test
    void deveLiberarAIniciativaDeQuemEntrouAoFimDoTurnoEEncaixarNaOrdem() throws Exception {
        iniciarComBromNoPrimeiroTurno();
        adicionarAoCombate("{\"jogadores\":[40]}");

        rolarIniciativa(sessaoCael, "NORMAL");
        assertEquals("Aguarde o fim do turno atual para rolar a iniciativa.", ultimoAviso(sessaoCael));

        enviar(sessaoBrom, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertFalse(participante(ultimoCombate(sessaoCael), "40").get("aguardandoFimDoTurno").asBoolean());

        faces.add(5);
        rolarIniciativa(sessaoCael, "NORMAL");

        JsonNode combate = ultimoCombate(sessaoMestre);
        assertEquals("Mira", combate.get("participantes").get(2).get("nomePersonagem").asText());
        assertEquals(20L, combate.get("turnoDe").asLong());
        enviar(sessaoAria, "{\"type\":\"COMBATE_PASSAR_TURNO\"}");
        assertEquals(40L, ultimoCombate(sessaoCael).get("turnoDe").asLong());
        assertEquals(1, ultimoCombate(sessaoCael).get("rodada").asInt());
    }

    @Test
    void deveNumerarOsMonstrosQueEntramDepoisContinuandoOGrupo() throws Exception {
        iniciarComAriaETresGoblins();

        adicionarAoCombate("{\"monstros\":[{\"personagemId\":500,\"quantidade\":2}]}");

        JsonNode entrada = ultimaEntrada(sessaoAria);
        assertEquals("Goblin 4", entrada.get("nomes").get(0).asText());
        assertEquals("Goblin 5", entrada.get("nomes").get(1).asText());
        assertEquals(6, ultimoCombate(sessaoMestre).get("participantes").size());
    }

    @Test
    void deveRecusarAdicaoSemNinguemOuFeitaPorJogador() throws Exception {
        iniciarComBromNoPrimeiroTurno();

        adicionarAoCombate("{}");
        assertEquals("Escolha ao menos um jogador ou monstro para entrar no combate.", ultimoAviso(sessaoMestre));

        enviar(sessaoAria, """
                {"type":"COMBATE_ADICIONAR","comandoCombate":{"jogadores":[40]}}
                """);
        assertEquals("Apenas o mestre pode administrar o combate.", ultimoAviso(sessaoAria));
        assertTrue(recebidas(sessaoMestre, "COMBATE_ENTRADA").isEmpty());
    }

    @Test
    void deveRecusarAnuncioDeEntradaEnviadoPeloCliente() throws Exception {
        enviar(sessaoAria, """
                {"type":"COMBATE_ENTRADA","entradaNoCombate":{"combateNome":"Falso","nomes":["Ninguem"]}}
                """);

        assertEquals("Tipo de mensagem não permitido.", ultimoAviso(sessaoAria));
        assertTrue(recebidas(sessaoMestre, "COMBATE_ENTRADA").isEmpty());
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
        rolarIniciativa(sessaoAria, "NORMAL");
        assertEquals("INICIATIVA", ultimoCombate(sessaoAria).get("fase").asText());

        faces.addAll(List.of(3, 18, 11));
        rolarMonstros("{\"modo\":\"NORMAL\"}");

        JsonNode combate = ultimoCombate(sessaoAria);
        assertEquals("EM_ANDAMENTO", combate.get("fase").asText());
        assertEquals(6, participante(combate, "monstro-1").get("iniciativa").get("total").asInt());
        assertEquals(21, participante(combate, "monstro-2").get("iniciativa").get("total").asInt());
        assertEquals(14, participante(combate, "monstro-3").get("iniciativa").get("total").asInt());
        assertEquals("monstro-2", combate.get("turnoDe").asText());
        assertEquals(10L, combate.get("controladorDoTurno").asLong());
        assertTrue(recebidas(sessaoCael, "SISTEMA").isEmpty());
    }

    @Test
    void deveRolarSoOsMonstrosEscolhidosComVantagemEBonusDeIniciativaDaFicha() throws Exception {
        iniciarComAriaETresGoblins();

        faces.addAll(List.of(4, 15));
        rolarMonstros("{\"entradas\":[\"monstro-2\"],\"modo\":\"VANTAGEM\"}");

        JsonNode combate = ultimoCombate(sessaoMestre);
        JsonNode iniciativa = participante(combate, "monstro-2").get("iniciativa");
        assertEquals(18, iniciativa.get("total").asInt());
        assertEquals(15, iniciativa.get("mantido").asInt());
        assertEquals(1, iniciativa.get("bonusIniciativa").asInt());
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
        rolarIniciativa(sessaoAria, "NORMAL");
        enviar(sessaoMestre, """
                {"type":"COMBATE_LIBERAR_REROLL","comandoCombate":{"entradas":["monstro-2"]}}
                """);
        faces.add(1);
        rolarMonstros("{\"entradas\":[\"monstro-2\"]}");

        assertEquals(4, participante(ultimoCombate(sessaoMestre), "monstro-2").get("iniciativa").get("total").asInt());
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
        rolarIniciativa(sessaoAria, "NORMAL");
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
        rolarIniciativa(sessaoAria, "NORMAL");
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
