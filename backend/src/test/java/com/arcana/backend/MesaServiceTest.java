package com.arcana.backend;

import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.ParticipanteRemovidoEvent;
import com.arcana.backend.table.ParticipantesAlteradosEvent;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.SolicitacaoEntrada;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.model.StatusSolicitacao;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.repository.SolicitacaoEntradaRepository;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private SolicitacaoEntradaRepository solicitacaoRepository;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 29, 15, 0);

    private final List<Object> eventosPublicados = new ArrayList<>();
    private final Set<Long> mesasComSessaoAtiva = new HashSet<>();
    private MesaService service;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new MesaService(mesaRepository, personagemRepository, solicitacaoRepository, relogio,
                eventosPublicados::add, mesasComSessaoAtiva::contains);
    }

    private SolicitacaoEntrada solicitacaoRecusadaHa(Duration tempoDesdeARecusa) {
        SolicitacaoEntrada solicitacao = new SolicitacaoEntrada(1L, 2L, "joao", 100L);
        solicitacao.setStatus(StatusSolicitacao.RECUSADA);
        solicitacao.setDecidedAt(AGORA.minus(tempoDesdeARecusa));
        return solicitacao;
    }

    @Test
    void devePermitirChatParaParticipanteDaMesa() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        var acesso = service.validarAcessoAoChat(1L, 2L);

        assertEquals(2L, acesso.userId());
        assertEquals("joao", acesso.displayName());
        assertEquals("JOGADOR", acesso.role());
    }

    @Test
    void deveNegarChatParaQuemNaoParticipaDaMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(AccessDeniedException.class, () -> service.validarAcessoAoChat(1L, 2L));
    }

    @Test
    void deveNegarChatDeMesaEncerradaMesmoParaParticipante() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.ENCERRADA);
        mesa.getParticipants().add(new Participante(1L, "gandalf", "MESTRE", null));
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class, () -> service.validarAcessoAoChat(1L, 1L));
    }

    @Test
    void deveNegarNovaSolicitacaoAntesDe15MinutosDaRecusa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(Optional.of(solicitacaoRecusadaHa(Duration.ofMinutes(5))));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));

        assertTrue(erro.getMessage().contains("10 minutos"));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void devePermitirNovaSolicitacaoApos15MinutosDaRecusa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(Optional.of(solicitacaoRecusadaHa(Duration.ofMinutes(15))));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));
        when(solicitacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L);

        assertEquals(StatusSolicitacao.PENDENTE, resultado.status());
    }

    @Test
    void deveInformarSegundosRestantesNaSolicitacaoRecusada() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(Optional.of(solicitacaoRecusadaHa(Duration.ofMinutes(14))));

        var resultado = service.buscarMinhaSolicitacao(1L, 2L);

        assertEquals(60, resultado.segundosAteNovaSolicitacao());
    }

    @Test
    void devePermitirMestreVerFichaDeParticipanteDaPropriaMesa() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));

        var ficha = service.buscarFichaDeParticipante(1L, 100L, 1L);

        assertEquals("Thorn", ficha.name());
    }

    @Test
    void deveNegarJogadorVerFichaDeOutroParticipante() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));
        mesa.getParticipants().add(new Participante(3L, "maria", "JOGADOR", 200L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.buscarFichaDeParticipante(1L, 100L, 3L));
        verify(personagemRepository, never()).findById(any());
    }

    @Test
    void deveNegarMestreVerFichaDePersonagemQueNaoEstaNaMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.buscarFichaDeParticipante(1L, 100L, 1L));
        verify(personagemRepository, never()).findById(any());
    }

    private Mesa mesaAbertaDoMestre(Long masterId) {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(masterId);
        mesa.setStatus(StatusMesa.ABERTA);
        return mesa;
    }

    private Personagem personagemDoJogador(Long ownerId, TipoJogador kind) {
        Usuario owner = new Usuario("joao", "joao@arcana.com", "hash");
        owner.setId(ownerId);

        Personagem personagem = new Personagem();
        personagem.setId(100L);
        personagem.setName("Thorn");
        personagem.setKind(kind);
        personagem.setLevel(1);
        personagem.setHpMax(10);
        personagem.setHpCurrent(10);
        personagem.setArmorClass(10);
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 10, 10));
        personagem.setOwner(owner);
        return personagem;
    }

    @Test
    void deveDerivarNomeDoMestreDoUsuarioAutenticado() {
        MesaRequestDto dto = new MesaRequestDto("Mesa dos Bravos", "Uma aventura épica");
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.criar(dto, 1L, "gandalf", PapelUsuario.MESTRE);

        assertEquals("gandalf", resultado.masterName());
        assertEquals(1L, resultado.masterId());
        assertEquals("MESTRE", resultado.participants().get(0).role());
        assertEquals("gandalf", resultado.participants().get(0).displayName());
    }

    @Test
    void deveNegarCriacaoDeMesaPorContaDeJogador() {
        MesaRequestDto dto = new MesaRequestDto("Mesa dos Bravos", "Uma aventura épica");

        assertThrows(AccessDeniedException.class,
                () -> service.criar(dto, 2L, "joao", PapelUsuario.JOGADOR));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePublicarEventoAoEncerrarMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizarStatus(1L, StatusMesa.ENCERRADA, 1L);

        assertEquals(List.of(new MesaStatusAlteradoEvent(1L, StatusMesa.ENCERRADA)), eventosPublicados);
    }

    @Test
    void devePermitirPausarERetomarAMesa() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizarStatus(1L, StatusMesa.PAUSADA, 1L);
        var retomada = service.atualizarStatus(1L, StatusMesa.EM_ANDAMENTO, 1L);

        assertEquals(StatusMesa.EM_ANDAMENTO, retomada.status());
        assertEquals(List.of(
                new MesaStatusAlteradoEvent(1L, StatusMesa.PAUSADA),
                new MesaStatusAlteradoEvent(1L, StatusMesa.EM_ANDAMENTO)), eventosPublicados);
    }

    @Test
    void deveNegarReaberturaDeMesaEncerrada() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.ENCERRADA);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        for (StatusMesa status : List.of(StatusMesa.ABERTA, StatusMesa.EM_ANDAMENTO, StatusMesa.PAUSADA)) {
            assertThrows(RegraDeNegocioException.class, () -> service.atualizarStatus(1L, status, 1L));
        }
        assertEquals(StatusMesa.ENCERRADA, mesa.getStatus());
        verify(mesaRepository, never()).save(any());
        assertTrue(eventosPublicados.isEmpty());
    }

    @Test
    void naoDevePublicarEventoQuandoStatusNaoMuda() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        service.atualizarStatus(1L, StatusMesa.ABERTA, 1L);

        verify(mesaRepository, never()).save(any());
        assertTrue(eventosPublicados.isEmpty());
    }

    @Test
    void deveNegarAlteracaoSemStatus() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(IllegalArgumentException.class, () -> service.atualizarStatus(1L, null, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoDeEntradaEmMesaPausada() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.PAUSADA);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void devePermitirChatDeParticipanteEmMesaPausadaInformandoOStatus() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.PAUSADA);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        var acesso = service.validarAcessoAoChat(1L, 2L);

        assertEquals(StatusMesa.PAUSADA, acesso.statusMesa());
    }

    @Test
    void devePermitirMestreExcluirMesaSemSessaoAtiva() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        service.deletar(1L, 1L);

        verify(mesaRepository).delete(mesa);
    }

    @Test
    void deveNegarExclusaoDeMesaComSessaoAtiva() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        mesasComSessaoAtiva.add(1L);

        assertThrows(RegraDeNegocioException.class, () -> service.deletar(1L, 1L));
        verify(mesaRepository, never()).delete(any());
    }

    @Test
    void deveNegarAlteracaoDeStatusPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RuntimeException.class,
                () -> service.atualizarStatus(1L, StatusMesa.EM_ANDAMENTO, 2L));
    }

    @Test
    void deveNegarExclusaoPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.deletar(1L, 2L));
        verify(mesaRepository, never()).delete(any());
    }

    @Test
    void deveCriarSolicitacaoPendenteSemAdicionarParticipante() {
        Mesa mesa = mesaAbertaDoMestre(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));
        when(solicitacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L);

        assertEquals(StatusSolicitacao.PENDENTE, resultado.status());
        assertEquals(100L, resultado.characterId());
        assertTrue(mesa.getParticipants().isEmpty());
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoDeContaDeMestre() {
        assertThrows(AccessDeniedException.class,
                () -> service.solicitarEntrada(1L, 3L, "saruman", PapelUsuario.MESTRE, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoQuandoMesaNaoEstaAberta() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.EM_ANDAMENTO);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoDeQuemJaParticipa() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSegundaSolicitacaoEnquantoHouverUmaPendente() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.existsByMesaIdAndUserIdAndStatus(1L, 2L, StatusSolicitacao.PENDENTE))
                .thenReturn(true);

        assertThrows(RegraDeNegocioException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoComPersonagemDeOutroUsuario() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(99L, TipoJogador.PLAYER)));

        assertThrows(AccessDeniedException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSolicitacaoComPersonagemQueNaoEhDoTipoJogador() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.MONSTER)));

        assertThrows(IllegalArgumentException.class,
                () -> service.solicitarEntrada(1L, 2L, "joao", PapelUsuario.JOGADOR, 100L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveRetornarUltimaSolicitacaoDoProprioUsuario() {
        SolicitacaoEntrada solicitacao = new SolicitacaoEntrada(1L, 2L, "joao", 100L);
        solicitacao.setStatus(StatusSolicitacao.RECUSADA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(Optional.of(solicitacao));

        var resultado = service.buscarMinhaSolicitacao(1L, 2L);

        assertEquals(StatusSolicitacao.RECUSADA, resultado.status());
    }

    @Test
    void deveNegarTrocaDePersonagemDeQuemNaoParticipa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(RegraDeNegocioException.class,
                () -> service.trocarPersonagem(1L, 2L, 100L));
        verify(mesaRepository, never()).save(any());
    }

    private SolicitacaoEntrada solicitacaoPendente(Long mesaId) {
        SolicitacaoEntrada solicitacao = new SolicitacaoEntrada(mesaId, 2L, "joao", 100L);
        solicitacao.setId(50L);
        return solicitacao;
    }

    @Test
    void deveListarSolicitacoesPendentesParaOMestre() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findByMesaIdAndStatusOrderByCreatedAtAsc(1L, StatusSolicitacao.PENDENTE))
                .thenReturn(List.of(solicitacaoPendente(1L)));

        var resultado = service.listarSolicitacoesPendentes(1L, 1L);

        assertEquals(1, resultado.size());
        assertEquals(StatusSolicitacao.PENDENTE, resultado.get(0).status());
    }

    @Test
    void deveNegarListagemDeSolicitacoesPorQuemNaoEhMestre() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(AccessDeniedException.class, () -> service.listarSolicitacoesPendentes(1L, 2L));
        verify(solicitacaoRepository, never()).findByMesaIdAndStatusOrderByCreatedAtAsc(any(), any());
    }

    @Test
    void deveAdicionarParticipanteAoAceitarSolicitacao() {
        Mesa mesa = mesaAbertaDoMestre(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(solicitacaoRepository.findById(50L)).thenReturn(Optional.of(solicitacaoPendente(1L)));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));
        when(solicitacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.aceitarSolicitacao(1L, 50L, 1L);

        assertEquals(StatusSolicitacao.ACEITA, resultado.status());
        assertNotNull(resultado.decidedAt());
        assertEquals(1, mesa.getParticipants().size());
        assertEquals(2L, mesa.getParticipants().get(0).getUserId());
        assertEquals(100L, mesa.getParticipants().get(0).getCharacterId());
        assertEquals(List.of(new ParticipantesAlteradosEvent(1L)), eventosPublicados);
        verify(mesaRepository).save(mesa);
    }

    @Test
    void deveRecusarSolicitacaoSemAdicionarParticipante() {
        Mesa mesa = mesaAbertaDoMestre(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(solicitacaoRepository.findById(50L)).thenReturn(Optional.of(solicitacaoPendente(1L)));
        when(solicitacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.recusarSolicitacao(1L, 50L, 1L);

        assertEquals(StatusSolicitacao.RECUSADA, resultado.status());
        assertNotNull(resultado.decidedAt());
        assertTrue(mesa.getParticipants().isEmpty());
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarDecisaoDeSolicitacaoPorQuemNaoEhMestre() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(AccessDeniedException.class, () -> service.aceitarSolicitacao(1L, 50L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.recusarSolicitacao(1L, 50L, 2L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarDecisaoDeSolicitacaoJaDecidida() {
        SolicitacaoEntrada solicitacao = solicitacaoPendente(1L);
        solicitacao.setStatus(StatusSolicitacao.RECUSADA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findById(50L)).thenReturn(Optional.of(solicitacao));

        assertThrows(RegraDeNegocioException.class, () -> service.aceitarSolicitacao(1L, 50L, 1L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarDecisaoDeSolicitacaoDeOutraMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));
        when(solicitacaoRepository.findById(50L)).thenReturn(Optional.of(solicitacaoPendente(9L)));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.aceitarSolicitacao(1L, 50L, 1L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarAceiteQuandoMesaNaoEstaMaisAberta() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.setStatus(StatusMesa.EM_ANDAMENTO);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(solicitacaoRepository.findById(50L)).thenReturn(Optional.of(solicitacaoPendente(1L)));

        assertThrows(RegraDeNegocioException.class, () -> service.aceitarSolicitacao(1L, 50L, 1L));
        assertTrue(mesa.getParticipants().isEmpty());
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveNegarSaidaDoMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class, () -> service.sair(1L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePermitirJogadorSairDaMesa() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", null));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.sair(1L, 2L);

        assertTrue(resultado.participants().isEmpty());
        assertEquals(List.of(new ParticipanteRemovidoEvent(1L, 2L, "joao", false)), eventosPublicados);
    }

    @Test
    void deveNegarSaidaDeQuemNaoParticipaDaMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(RegraDeNegocioException.class, () -> service.sair(1L, 2L));
        verify(mesaRepository, never()).save(any());
        assertTrue(eventosPublicados.isEmpty());
    }

    @Test
    void devePermitirMestreRemoverJogadorDaMesa() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(1L, "gandalf", "MESTRE", null));
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.removerParticipante(1L, 2L, 1L);

        assertEquals(1, resultado.participants().size());
        assertEquals(1L, resultado.participants().get(0).userId());
        assertEquals(List.of(new ParticipanteRemovidoEvent(1L, 2L, "joao", true)), eventosPublicados);
    }

    @Test
    void deveNegarJogadorRemoverOutroParticipante() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 100L));
        mesa.getParticipants().add(new Participante(3L, "maria", "JOGADOR", 200L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.removerParticipante(1L, 2L, 3L));
        assertEquals(2, mesa.getParticipants().size());
        assertTrue(eventosPublicados.isEmpty());
    }

    @Test
    void deveNegarMestreRemoverASiMesmo() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(1L, "gandalf", "MESTRE", null));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class, () -> service.removerParticipante(1L, 1L, 1L));
        assertEquals(1, mesa.getParticipants().size());
    }

    @Test
    void deveInformarQuandoUsuarioRemovidoNaoParticipaDaMesa() {
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesaAbertaDoMestre(1L)));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.removerParticipante(1L, 2L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePermitirParticipanteTrocarParaOutroPersonagemProprio() {
        Mesa mesa = mesaAbertaDoMestre(1L);
        mesa.getParticipants().add(new Participante(2L, "joao", "JOGADOR", 50L));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.trocarPersonagem(1L, 2L, 100L);

        assertEquals(1, resultado.participants().size());
        assertEquals(100L, resultado.participants().get(0).characterId());
        assertEquals(List.of(new ParticipantesAlteradosEvent(1L)), eventosPublicados);
    }

    @Test
    void devePermitirMestreAssociarNpcProprio() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(1L, TipoJogador.NPC_FRIENDLY)));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.adicionarNpc(1L, 100L, 1L);

        assertTrue(resultado.npcIds().contains(100L));
    }

    @Test
    void deveNegarAssociarNpcPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.adicionarNpc(1L, 100L, 2L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarAssociarNpcQuePertenceAOutroUsuario() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(99L, TipoJogador.NPC_FRIENDLY)));

        assertThrows(AccessDeniedException.class, () -> service.adicionarNpc(1L, 100L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarAssociarPersonagemDoTipoJogadorComoNpc() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(1L, TipoJogador.PLAYER)));

        assertThrows(IllegalArgumentException.class, () -> service.adicionarNpc(1L, 100L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePermitirMestreRemoverNpc() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.getNpcIds().add(100L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.removerNpc(1L, 100L, 1L);

        assertFalse(resultado.npcIds().contains(100L));
    }
}
