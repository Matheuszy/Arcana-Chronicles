package com.arcana.backend;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.chat.desafio.DesafioDto;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.chat.desafio.EscolhaTeste;
import com.arcana.backend.chat.desafio.EstadoDesafio;
import com.arcana.backend.chat.desafio.MensagemDeResultado;
import com.arcana.backend.chat.desafio.NovoDesafio;
import com.arcana.backend.chat.dice.ModoRolagem;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.FichaDeParticipante;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.service.MesaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DesafioServiceTest {

    private static final Long MESA_ID = 1L;
    private static final AcessoMesaDto MESTRE = acesso(10L, "Mestre", "MESTRE");
    private static final AcessoMesaDto ARIA = acesso(20L, "Aria", "JOGADOR");
    private static final AcessoMesaDto BROM = acesso(30L, "Brom", "JOGADOR");
    private static final AcessoMesaDto CAEL = acesso(40L, "Cael", "JOGADOR");

    @Mock
    private MesaService mesaService;

    private final Deque<Integer> faces = new ArrayDeque<>();
    private MesaAtiva mesa;
    private DesafioService service;

    @BeforeEach
    void setUp() {
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
        service = new DesafioService(mesaService, new RolagemDadosService(dados), relogio);
        mesa = new MesaAtiva(MESA_ID, StatusMesa.EM_ANDAMENTO);

        lenient().when(mesaService.buscarFichaDoJogadorNaMesa(eq(MESA_ID), eq(ARIA.userId())))
                .thenReturn(ficha(ARIA, "Lyra", Map.of(Atributo.DESTREZA, 16), Map.of(), Map.of(Pericia.FURTIVIDADE, 4)));
        lenient().when(mesaService.buscarFichaDoJogadorNaMesa(eq(MESA_ID), eq(BROM.userId())))
                .thenReturn(ficha(BROM, "Thorn", Map.of(Atributo.FORCA, 14), Map.of(Atributo.FORCA, 2), Map.of()));
    }

    private static AcessoMesaDto acesso(Long userId, String nome, String papel) {
        return new AcessoMesaDto(MESA_ID, userId, nome, papel, StatusMesa.EM_ANDAMENTO);
    }

    private static FichaDeParticipante ficha(AcessoMesaDto jogador, String personagem, Map<Atributo, Integer> valores,
                                             Map<Atributo, Integer> bonusAtributos, Map<Pericia, Integer> bonusPericias) {
        Map<Atributo, Integer> todos = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            todos.put(atributo, valores.getOrDefault(atributo, 10));
        }
        return new FichaDeParticipante(jogador.userId(), jogador.displayName(), 100L + jogador.userId(), personagem,
                todos, bonusAtributos, bonusPericias, 2);
    }

    private void proximasFaces(Integer... valores) {
        faces.addAll(List.of(valores));
    }

    private DesafioDto desafioPara(int cd, NovoDesafio.Convocacao... convocacoes) {
        return service.criar(mesa, MESTRE, new NovoDesafio(cd, "Escalar o muro", List.of(convocacoes)));
    }

    private NovoDesafio.Convocacao convocar(AcessoMesaDto jogador, ModoRolagem modo) {
        return new NovoDesafio.Convocacao(jogador.userId(), modo);
    }

    private DesafioDto.ParticipacaoDto participacao(DesafioDto desafio, AcessoMesaDto jogador) {
        return desafio.jogadores().stream().filter(p -> p.jogadorId().equals(jogador.userId())).findFirst().orElseThrow();
    }

    @Test
    void deveCriarDesafioComCdModoPorJogadorENomesDaFicha() {
        DesafioDto desafio = desafioPara(15, convocar(ARIA, ModoRolagem.VANTAGEM), convocar(BROM, ModoRolagem.DESVANTAGEM));

        assertEquals(15, desafio.cd());
        assertEquals("Escalar o muro", desafio.descricao());
        assertTrue(desafio.ativo());
        assertEquals(ModoRolagem.VANTAGEM, participacao(desafio, ARIA).modo());
        assertEquals(ModoRolagem.DESVANTAGEM, participacao(desafio, BROM).modo());
        assertEquals("Lyra", participacao(desafio, ARIA).nomePersonagem());
        assertEquals(EstadoDesafio.AGUARDANDO_ROLAGEM, participacao(desafio, BROM).estado());
    }

    @Test
    void deveNegarCriacaoDeDesafioPorJogador() {
        assertThrows(AccessDeniedException.class, () ->
                service.criar(mesa, ARIA, new NovoDesafio(15, null, List.of(convocar(BROM, ModoRolagem.NORMAL)))));
    }

    @Test
    void deveValidarCdDescricaoEJogadoresDoDesafio() {
        assertThrows(IllegalArgumentException.class, () -> desafioPara(0, convocar(ARIA, ModoRolagem.NORMAL)));
        assertThrows(IllegalArgumentException.class, () -> desafioPara(41, convocar(ARIA, ModoRolagem.NORMAL)));
        assertThrows(IllegalArgumentException.class, () -> desafioPara(15));
        assertThrows(IllegalArgumentException.class, () ->
                desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL), convocar(ARIA, ModoRolagem.VANTAGEM)));
        assertThrows(IllegalArgumentException.class, () ->
                service.criar(mesa, MESTRE, new NovoDesafio(15, "x".repeat(121), List.of(convocar(ARIA, ModoRolagem.NORMAL)))));
    }

    @Test
    void deveRecusarDesafioParaQuemNaoEJogadorDaMesa() {
        when(mesaService.buscarFichaDoJogadorNaMesa(MESA_ID, CAEL.userId()))
                .thenThrow(new RegraDeNegocioException("Este jogador não participa da mesa."));

        assertThrows(RegraDeNegocioException.class, () -> desafioPara(15, convocar(CAEL, ModoRolagem.NORMAL)));
        assertFalse(service.jogadorBloqueado(mesa, CAEL.userId()));
    }

    @Test
    void deveSerSucessoQuandoTotalForIgualACd() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL));
        proximasFaces(12);

        var resultado = service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));

        assertEquals(15, resultado.rolagem().total());
        assertEquals("1d20+3", resultado.rolagem().dados().getExpression());
        assertEquals("Destreza", resultado.rolagem().descricaoEscolha());
        assertTrue(resultado.rolagem().sucesso());
        assertEquals(EstadoDesafio.SUCESSO, participacao(resultado.desafio(), ARIA).estado());
        assertFalse(resultado.desafio().ativo());
        assertFalse(service.jogadorBloqueado(mesa, ARIA.userId()));
    }

    @Test
    void deveFalharQuandoTotalForMenorQueCdEAguardarDecisaoDoMestre() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL));
        proximasFaces(11);

        var resultado = service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));

        assertEquals(14, resultado.rolagem().total());
        assertFalse(resultado.rolagem().sucesso());
        assertEquals(EstadoDesafio.AGUARDANDO_DECISAO_DO_MESTRE, participacao(resultado.desafio(), ARIA).estado());
        assertTrue(service.jogadorBloqueado(mesa, ARIA.userId()));
        assertThrows(IllegalStateException.class, () ->
                service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA)));
    }

    @Test
    void deveUsarModoDefinidoPeloMestreEBonusIndividualDaPericia() {
        desafioPara(18, convocar(ARIA, ModoRolagem.DESVANTAGEM));
        proximasFaces(17, 6);

        var resultado = service.rolar(mesa, ARIA, EscolhaTeste.dePericia(Pericia.FURTIVIDADE));

        assertEquals(List.of(17, 6), resultado.rolagem().dados().getRolls());
        assertEquals(6, resultado.rolagem().dados().getKept());
        assertEquals(13, resultado.rolagem().total());
        assertEquals("Furtividade (Destreza)", resultado.rolagem().descricaoEscolha());
    }

    @Test
    void devePermitirUmaUnicaTentativaExtraComModoDoMestreEEscolhaLivreDoJogador() {
        String desafioId = desafioPara(16, convocar(BROM, ModoRolagem.NORMAL)).id();
        proximasFaces(3);
        service.rolar(mesa, BROM, EscolhaTeste.deAtributo(Atributo.FORCA));

        DesafioDto liberado = service.liberarTentativaExtra(mesa, MESTRE, desafioId, String.valueOf(BROM.userId()), ModoRolagem.VANTAGEM);
        assertEquals(EstadoDesafio.AGUARDANDO_TENTATIVA_EXTRA, participacao(liberado, BROM).estado());
        assertEquals(ModoRolagem.VANTAGEM, participacao(liberado, BROM).modo());

        proximasFaces(4, 15);
        var extra = service.rolar(mesa, BROM, EscolhaTeste.dePericia(Pericia.ATLETISMO));

        assertEquals(2, extra.rolagem().tentativa());
        assertEquals(17, extra.rolagem().total());
        assertTrue(extra.rolagem().sucesso());
        assertEquals(EstadoDesafio.SUCESSO, participacao(extra.desafio(), BROM).estado());
        assertEquals(2, participacao(extra.desafio(), BROM).rolagens().size());
    }

    @Test
    void naoDeveExistirTerceiraTentativa() {
        String desafioId = desafioPara(20, convocar(BROM, ModoRolagem.NORMAL)).id();
        proximasFaces(2);
        service.rolar(mesa, BROM, EscolhaTeste.deAtributo(Atributo.FORCA));
        service.liberarTentativaExtra(mesa, MESTRE, desafioId, String.valueOf(BROM.userId()), ModoRolagem.NORMAL);
        proximasFaces(5);

        var extra = service.rolar(mesa, BROM, EscolhaTeste.deAtributo(Atributo.FORCA));

        assertEquals(EstadoDesafio.FALHA, participacao(extra.desafio(), BROM).estado());
        assertFalse(service.jogadorBloqueado(mesa, BROM.userId()));
        assertThrows(IllegalStateException.class, () ->
                service.liberarTentativaExtra(mesa, MESTRE, desafioId, String.valueOf(BROM.userId()), ModoRolagem.NORMAL));
        assertThrows(IllegalStateException.class, () ->
                service.rolar(mesa, BROM, EscolhaTeste.deAtributo(Atributo.FORCA)));
    }

    @Test
    void deveExigirModoAoLiberarTentativaExtraEPrimeiraRolagemComFalha() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL)).id();

        assertThrows(IllegalStateException.class, () ->
                service.liberarTentativaExtra(mesa, MESTRE, desafioId, String.valueOf(ARIA.userId()), ModoRolagem.NORMAL));
        assertThrows(IllegalArgumentException.class, () ->
                service.liberarTentativaExtra(mesa, MESTRE, desafioId, String.valueOf(ARIA.userId()), null));
    }

    @Test
    void deveEncerrarComFalhaQuandoMestreNegaNovaTentativa() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL)).id();
        proximasFaces(1);
        var primeira = service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));
        assertTrue(primeira.rolagem().dados().isFumble());

        DesafioDto encerrado = service.encerrarComFalha(mesa, MESTRE, desafioId, String.valueOf(ARIA.userId()));

        assertEquals(EstadoDesafio.FALHA, participacao(encerrado, ARIA).estado());
        assertFalse(service.jogadorBloqueado(mesa, ARIA.userId()));
    }

    @Test
    void deveNegarDecisoesDoDesafioParaJogadores() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL)).id();
        proximasFaces(2);
        service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));

        assertThrows(AccessDeniedException.class, () ->
                service.liberarTentativaExtra(mesa, ARIA, desafioId, String.valueOf(ARIA.userId()), ModoRolagem.VANTAGEM));
        assertThrows(AccessDeniedException.class, () -> service.encerrarComFalha(mesa, BROM, desafioId, String.valueOf(ARIA.userId())));
        assertThrows(AccessDeniedException.class, () -> service.cancelar(mesa, ARIA, desafioId, null));
    }

    @Test
    void devePermitirApenasUmDesafioAtivoPorJogador() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL));

        assertThrows(IllegalStateException.class, () ->
                desafioPara(12, convocar(BROM, ModoRolagem.NORMAL), convocar(ARIA, ModoRolagem.NORMAL)));
        assertFalse(service.jogadorBloqueado(mesa, BROM.userId()));
        assertTrue(service.jogadorBloqueado(mesa, ARIA.userId()));
    }

    @Test
    void deveCancelarIndividualmenteOuODesafioInteiro() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL), convocar(BROM, ModoRolagem.NORMAL)).id();

        DesafioDto parcial = service.cancelar(mesa, MESTRE, desafioId, String.valueOf(ARIA.userId()));
        assertEquals(EstadoDesafio.CANCELADO, participacao(parcial, ARIA).estado());
        assertEquals(EstadoDesafio.AGUARDANDO_ROLAGEM, participacao(parcial, BROM).estado());
        assertFalse(service.jogadorBloqueado(mesa, ARIA.userId()));

        DesafioDto total = service.cancelar(mesa, MESTRE, desafioId, null);
        assertEquals(EstadoDesafio.CANCELADO, participacao(total, BROM).estado());
        assertFalse(total.ativo());
        assertThrows(IllegalStateException.class, () -> service.cancelar(mesa, MESTRE, desafioId, null));
    }

    @Test
    void deveCongelarDesafioComMesaPausadaEContinuarAoRetomar() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL));
        mesa.alterarStatus(StatusMesa.PAUSADA);

        assertThrows(IllegalStateException.class, () ->
                service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA)));
        assertThrows(IllegalStateException.class, () -> desafioPara(10, convocar(BROM, ModoRolagem.NORMAL)));
        assertTrue(service.jogadorBloqueado(mesa, ARIA.userId()));

        mesa.alterarStatus(StatusMesa.EM_ANDAMENTO);
        proximasFaces(20);
        var resultado = service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));

        assertTrue(resultado.rolagem().dados().isCritical());
        assertEquals(EstadoDesafio.SUCESSO, participacao(resultado.desafio(), ARIA).estado());
    }

    @Test
    void devePermitirCancelarDesafioComMesaPausada() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL)).id();
        mesa.alterarStatus(StatusMesa.PAUSADA);

        DesafioDto cancelado = service.cancelar(mesa, MESTRE, desafioId, null);

        assertEquals(EstadoDesafio.CANCELADO, participacao(cancelado, ARIA).estado());
        assertFalse(service.jogadorBloqueado(mesa, ARIA.userId()));
    }

    @Test
    void deveCancelarAutomaticamenteNaSaidaDoJogadorENaAusenciaDoMestre() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL), convocar(BROM, ModoRolagem.NORMAL));

        List<DesafioDto> naSaida = service.cancelarDoJogador(mesa, ARIA.userId());
        assertEquals(1, naSaida.size());
        assertEquals(EstadoDesafio.CANCELADO, participacao(naSaida.get(0), ARIA).estado());
        assertTrue(service.jogadorBloqueado(mesa, BROM.userId()));

        List<DesafioDto> naAusencia = service.cancelarPorAusenciaDoMestre(mesa);
        assertEquals(EstadoDesafio.CANCELADO, participacao(naAusencia.get(0), BROM).estado());
        assertFalse(service.jogadorBloqueado(mesa, BROM.userId()));
        assertTrue(service.cancelarPorAusenciaDoMestre(mesa).isEmpty());
    }

    @Test
    void deveMostrarDesafioApenasParaOMestreEOsJogadoresEnvolvidos() {
        desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL));

        assertEquals(1, service.visiveisPara(mesa, MESTRE.userId()).size());
        assertEquals(1, service.visiveisPara(mesa, ARIA.userId()).size());
        assertTrue(service.visiveisPara(mesa, BROM.userId()).isEmpty());
        assertTrue(service.visiveisPara(mesa, CAEL.userId()).isEmpty());
    }

    private String desafioComAriaVencendoEBromFalhando() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL), convocar(BROM, ModoRolagem.NORMAL)).id();
        proximasFaces(12, 2);
        service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));
        service.rolar(mesa, BROM, EscolhaTeste.deAtributo(Atributo.FORCA));
        service.encerrarComFalha(mesa, MESTRE, desafioId, String.valueOf(BROM.userId()));
        return desafioId;
    }

    @Test
    void deveEntregarMensagemDosVencedoresApenasAQuemVenceu() {
        String desafioId = desafioComAriaVencendoEBromFalhando();

        var enviada = service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.VENCEDORES, null, "  Você alcança o topo do muro.  ");

        assertEquals("Você alcança o topo do muro.", enviada.mensagem().texto());
        assertEquals(Set.of(ARIA.userId()), enviada.mensagem().destinatarios());
        assertEquals(1, service.visaoPara(mesa, desafioId, ARIA.userId()).mensagens().size());
        assertTrue(service.visaoPara(mesa, desafioId, BROM.userId()).mensagens().isEmpty());
        assertEquals(Set.of(ARIA.userId()), enviada.desafio().mensagens().get(0).destinatarios());
        assertNull(service.visaoPara(mesa, desafioId, ARIA.userId()).mensagens().get(0).destinatarios());
    }

    @Test
    void deveEntregarMensagemDosDerrotadosEIndividualSoAosEscolhidos() {
        String desafioId = desafioComAriaVencendoEBromFalhando();

        service.enviarMensagem(mesa, MESTRE, desafioId, MensagemDeResultado.Destino.DERROTADOS, null, "Você escorrega.");
        service.enviarMensagem(mesa, MESTRE, desafioId, MensagemDeResultado.Destino.JOGADOR, String.valueOf(ARIA.userId()), "Há uma chave no topo.");

        assertEquals(List.of("Você escorrega."),
                service.visaoPara(mesa, desafioId, BROM.userId()).mensagens().stream().map(DesafioDto.MensagemDto::texto).toList());
        assertEquals(List.of("Há uma chave no topo."),
                service.visaoPara(mesa, desafioId, ARIA.userId()).mensagens().stream().map(DesafioDto.MensagemDto::texto).toList());
        assertEquals(2, service.visaoPara(mesa, desafioId, MESTRE.userId()).mensagens().size());
        assertThrows(IllegalArgumentException.class, () -> service.visaoPara(mesa, desafioId, CAEL.userId()));
    }

    @Test
    void deveRecusarMensagemAntesDoResultadoOuSemDestinatarios() {
        String desafioId = desafioPara(15, convocar(ARIA, ModoRolagem.NORMAL), convocar(BROM, ModoRolagem.NORMAL)).id();
        proximasFaces(12);
        service.rolar(mesa, ARIA, EscolhaTeste.deAtributo(Atributo.DESTREZA));

        assertThrows(IllegalStateException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.JOGADOR, String.valueOf(BROM.userId()), "Ainda não."));
        assertThrows(IllegalStateException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.DERROTADOS, null, "Ninguém falhou."));
        assertThrows(IllegalArgumentException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.JOGADOR, String.valueOf(CAEL.userId()), "Fora do desafio."));
    }

    @Test
    void deveValidarTextoEPermissaoDaMensagem() {
        String desafioId = desafioComAriaVencendoEBromFalhando();

        assertThrows(IllegalArgumentException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.VENCEDORES, null, "   "));
        assertThrows(IllegalArgumentException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.VENCEDORES, null, "x".repeat(121)));
        assertThrows(IllegalArgumentException.class, () -> service.enviarMensagem(mesa, MESTRE, desafioId,
                null, null, "Sem destino."));
        assertThrows(AccessDeniedException.class, () -> service.enviarMensagem(mesa, ARIA, desafioId,
                MensagemDeResultado.Destino.DERROTADOS, null, "Ha!"));
        assertEquals(120, service.enviarMensagem(mesa, MESTRE, desafioId,
                MensagemDeResultado.Destino.VENCEDORES, null, "x".repeat(120)).mensagem().texto().length());
    }

    @Test
    void deveExigirExatamenteUmAtributoOuUmaPericiaNaEscolha() {
        assertThrows(IllegalArgumentException.class, () -> new EscolhaTeste(null, null));
        assertThrows(IllegalArgumentException.class, () -> new EscolhaTeste(Atributo.FORCA, Pericia.ATLETISMO));
    }

    @Test
    void deveRecusarRolagemDeQuemNaoTemDesafio() {
        assertThrows(IllegalStateException.class, () ->
                service.rolar(mesa, CAEL, EscolhaTeste.deAtributo(Atributo.FORCA)));
    }
}
