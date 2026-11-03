package com.arcana.backend;

import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.friend.AmizadeDesfeitaEvent;
import com.arcana.backend.friend.dto.response.AmigoDto;
import com.arcana.backend.friend.dto.response.SolicitacaoAmizadeResponseDto;
import com.arcana.backend.friend.dto.response.UsuarioBloqueadoDto;
import com.arcana.backend.friend.dto.response.UsuarioEncontradoDto;
import com.arcana.backend.friend.model.BloqueioUsuario;
import com.arcana.backend.friend.model.RelacaoDeAmizade;
import com.arcana.backend.friend.model.SolicitacaoAmizade;
import com.arcana.backend.friend.model.StatusSolicitacaoAmizade;
import com.arcana.backend.friend.repository.BloqueioUsuarioRepository;
import com.arcana.backend.friend.repository.SolicitacaoAmizadeRepository;
import com.arcana.backend.friend.service.AmizadeService;
import com.arcana.backend.presence.NotificacaoDeUsuarioEvent;
import com.arcana.backend.presence.PresencaService;
import com.arcana.backend.presence.model.EstadoPresenca;
import com.arcana.backend.presence.model.MensagemDoCanal;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AmizadeServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 12, 0);

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    @Mock
    private SolicitacaoAmizadeRepository solicitacaoRepository;

    @Mock
    private BloqueioUsuarioRepository bloqueioRepository;

    @Mock
    private PresencaService presencaService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AmizadeService service;

    private final Usuario aria = usuario(1L, "aria", PapelUsuario.JOGADOR);
    private final Usuario brom = usuario(2L, "brom", PapelUsuario.MESTRE);
    private final Usuario cael = usuario(3L, "cael", PapelUsuario.JOGADOR);

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new AmizadeService(usuarioRepositorie, solicitacaoRepository, bloqueioRepository,
                presencaService, eventPublisher, relogio);
    }

    @Test
    void deveListarAmigosEmOrdemAlfabeticaComAPresencaDeCadaUm() {
        SolicitacaoAmizade comCael = solicitacao(7L, cael, aria, StatusSolicitacaoAmizade.ACEITA);
        SolicitacaoAmizade comBrom = solicitacao(8L, aria, brom, StatusSolicitacaoAmizade.ACEITA);
        comBrom.setDecidedAt(AGORA.minusHours(2));
        when(solicitacaoRepository.findAmizadesDe(1L, StatusSolicitacaoAmizade.ACEITA))
                .thenReturn(List.of(comCael, comBrom));
        when(presencaService.estadoDe(2L)).thenReturn(EstadoPresenca.EM_MESA);
        when(presencaService.estadoDe(3L)).thenReturn(EstadoPresenca.OFFLINE);

        List<AmigoDto> amigos = service.listarAmigos(1L);

        assertEquals(List.of(
                new AmigoDto(2L, "brom", PapelUsuario.MESTRE, EstadoPresenca.EM_MESA, AGORA.minusHours(2)),
                new AmigoDto(3L, "cael", PapelUsuario.JOGADOR, EstadoPresenca.OFFLINE, null)), amigos);
    }

    @Test
    void deveRemoverAmizadeAvisandoOsDoisUsuarios() {
        SolicitacaoAmizade amizade = solicitacao(8L, aria, brom, StatusSolicitacaoAmizade.ACEITA);
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.of(amizade));

        service.removerAmizade(1L, 2L);

        verify(solicitacaoRepository).delete(amizade);
        verify(eventPublisher).publishEvent(new AmizadeDesfeitaEvent(1L, 2L));
        verify(eventPublisher).publishEvent(
                NotificacaoDeUsuarioEvent.para(MensagemDoCanal.amizadesAlteradas(), 1L, 2L));
    }

    @Test
    void deveAvisarQuandoTentarRemoverQuemNaoEAmigo() {
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.removerAmizade(1L, 2L));
        verify(solicitacaoRepository, never()).delete(any());
    }

    @Test
    void deveBloquearDesfazendoAAmizade() {
        SolicitacaoAmizade amizade = solicitacao(8L, brom, aria, StatusSolicitacaoAmizade.ACEITA);
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(aria));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.of(amizade));
        when(bloqueioRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioBloqueadoDto bloqueado = service.bloquear(1L, 2L);

        assertEquals(2L, bloqueado.usuarioId());
        assertEquals(AGORA, bloqueado.desde());
        verify(solicitacaoRepository).delete(amizade);
        verify(eventPublisher).publishEvent(new AmizadeDesfeitaEvent(1L, 2L));
    }

    @Test
    void deveBloquearCancelandoPedidoPendenteEmQualquerSentido() {
        SolicitacaoAmizade pedido = solicitacao(9L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(aria));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.of(pedido));
        when(bloqueioRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.bloquear(1L, 2L);

        assertEquals(StatusSolicitacaoAmizade.CANCELADA, pedido.getStatus());
        verify(solicitacaoRepository).save(pedido);
        verify(solicitacaoRepository, never()).delete(any());
    }

    @Test
    void deveImpedirBloquearASiMesmoOuBloquearDuasVezes() {
        assertThrows(RegraDeNegocioException.class, () -> service.bloquear(1L, 1L));

        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(1L, 2L)).thenReturn(true);
        assertThrows(RegraDeNegocioException.class, () -> service.bloquear(1L, 2L));
        verify(bloqueioRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveImpedirPedidoParaQuemOUsuarioBloqueou() {
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(2L, 1L)).thenReturn(false);
        when(bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(1L, 2L)).thenReturn(true);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> service.enviar(1L, 2L));

        assertEquals("Você bloqueou este usuário. Desbloqueie para enviar um pedido de amizade.", erro.getMessage());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveResponderComoUsuarioInexistenteQuandoODestinatarioBloqueouORemetente() {
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(2L, 1L)).thenReturn(true);

        RecursoNaoEncontradoException erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.enviar(1L, 2L));

        assertEquals("Usuário não encontrado: 2", erro.getMessage());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveOcultarDaBuscaUsuariosBloqueadosEmQualquerSentido() {
        when(bloqueioRepository.findIdsBloqueadosEmQualquerSentido(1L)).thenReturn(List.of(2L));
        when(usuarioRepositorie.findByCodigoAmigo("BBBBCCCC")).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc(
                "BBBBCCCC", Set.of(1L, 2L))).thenReturn(List.of());

        assertTrue(service.buscar(1L, "BBBBCCCC").isEmpty());
    }

    @Test
    void deveDesbloquearApenasQuemFoiBloqueadoPeloUsuario() {
        BloqueioUsuario bloqueio = new BloqueioUsuario(aria, brom, AGORA);
        when(bloqueioRepository.findByBloqueadorIdAndBloqueadoId(1L, 2L)).thenReturn(Optional.of(bloqueio));
        when(bloqueioRepository.findByBloqueadorIdAndBloqueadoId(2L, 1L)).thenReturn(Optional.empty());

        service.desbloquear(1L, 2L);

        verify(bloqueioRepository).delete(bloqueio);
        assertThrows(RecursoNaoEncontradoException.class, () -> service.desbloquear(2L, 1L));
    }

    @Test
    void deveAvisarOsDoisUsuariosQuandoUmPedidoEAceito() {
        SolicitacaoAmizade pedido = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(solicitacaoRepository.save(pedido)).thenReturn(pedido);

        service.aceitar(5L, 1L);

        verify(eventPublisher).publishEvent(
                NotificacaoDeUsuarioEvent.para(MensagemDoCanal.amizadesAlteradas(), 2L, 1L));
    }

    @Test
    void deveRecusarBuscaComMenosDeTresCaracteresIgnorandoEspacos() {
        assertThrows(IllegalArgumentException.class, () -> service.buscar(1L, "  ab  "));
        assertThrows(IllegalArgumentException.class, () -> service.buscar(1L, null));
        verifyNoInteractions(usuarioRepositorie);
    }

    @Test
    void deveRecusarBuscaMaiorQueOLimite() {
        assertThrows(IllegalArgumentException.class, () -> service.buscar(1L, "x".repeat(101)));
        verifyNoInteractions(usuarioRepositorie);
    }

    @Test
    void deveBuscarPorTrechoDoNomeIndicandoARelacaoComCadaUsuario() {
        when(usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc("rom", Set.of(1L)))
                .thenReturn(List.of(brom, cael));
        SolicitacaoAmizade pedido = solicitacao(10L, cael, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findEntreUsuarioEOutros(eq(1L), eq(List.of(2L, 3L)), anyCollection()))
                .thenReturn(List.of(pedido));

        List<UsuarioEncontradoDto> resultado = service.buscar(1L, " rom ");

        assertEquals(2, resultado.size());
        assertEquals(new UsuarioEncontradoDto(2L, "brom", PapelUsuario.MESTRE, RelacaoDeAmizade.NENHUMA, null),
                resultado.get(0));
        assertEquals(new UsuarioEncontradoDto(3L, "cael", PapelUsuario.JOGADOR, RelacaoDeAmizade.PEDIDO_RECEBIDO, 10L),
                resultado.get(1));
    }

    @Test
    void deveBuscarPeloCodigoDeAmigoEmQualquerFormatacao() {
        when(usuarioRepositorie.findByCodigoAmigo("7K9MX2QP")).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc(anyString(), any()))
                .thenReturn(List.of());
        SolicitacaoAmizade amizade = solicitacao(11L, aria, brom, StatusSolicitacaoAmizade.ACEITA);
        when(solicitacaoRepository.findEntreUsuarioEOutros(eq(1L), eq(List.of(2L)), anyCollection()))
                .thenReturn(List.of(amizade));

        List<UsuarioEncontradoDto> resultado = service.buscar(1L, "arc-7k9m-x2qp");

        assertEquals(1, resultado.size());
        assertEquals(RelacaoDeAmizade.AMIGOS, resultado.get(0).relacao());
    }

    @Test
    void deveColocarOResultadoDoCodigoPrimeiroSemRepetirNemPassarDeDezResultados() {
        List<Usuario> porNome = new java.util.ArrayList<>();
        for (long id = 10; id < 20; id++) {
            porNome.add(usuario(id, "nome" + id, PapelUsuario.JOGADOR));
        }
        porNome.set(3, brom);
        when(usuarioRepositorie.findByCodigoAmigo("BBBBCCCC")).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc("BBBBCCCC", Set.of(1L)))
                .thenReturn(porNome);

        List<UsuarioEncontradoDto> resultado = service.buscar(1L, "BBBBCCCC");

        assertEquals(10, resultado.size());
        assertEquals(2L, resultado.get(0).id());
        assertEquals(1, resultado.stream().filter(u -> u.id().equals(2L)).count());
    }

    @Test
    void naoDeveIncluirOProprioUsuarioAoBuscarPeloSeuCodigo() {
        when(usuarioRepositorie.findByCodigoAmigo("AAAABBBB")).thenReturn(Optional.of(aria));
        when(usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc("AAAABBBB", Set.of(1L)))
                .thenReturn(List.of());

        assertTrue(service.buscar(1L, "AAAABBBB").isEmpty());
        verifyNoInteractions(solicitacaoRepository);
    }

    @Test
    void deveEnviarPedidoDeAmizadePendente() {
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(aria));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.empty());
        when(solicitacaoRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoAmizadeResponseDto resposta = service.enviar(1L, 2L);

        assertEquals(StatusSolicitacaoAmizade.PENDENTE, resposta.status());
        assertEquals(1L, resposta.remetenteId());
        assertEquals(2L, resposta.destinatarioId());
        assertEquals(AGORA, resposta.createdAt());
    }

    @Test
    void deveImpedirPedidoDeAmizadeParaSiMesmo() {
        assertThrows(RegraDeNegocioException.class, () -> service.enviar(1L, 1L));
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveAvisarQuandoODestinatarioNaoExiste() {
        when(usuarioRepositorie.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.enviar(1L, 9L));
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveImpedirPedidoDuplicado() {
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection()))
                .thenReturn(Optional.of(solicitacao(5L, aria, brom, StatusSolicitacaoAmizade.PENDENTE)));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> service.enviar(1L, 2L));

        assertEquals("Você já enviou um pedido de amizade para este usuário.", erro.getMessage());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveImpedirPedidoEntreQuemJaEAmigo() {
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection()))
                .thenReturn(Optional.of(solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.ACEITA)));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> service.enviar(1L, 2L));

        assertEquals("Vocês já são amigos.", erro.getMessage());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveAceitarAutomaticamenteQuandoODestinatarioJaHaviaPedidoAmizade() {
        SolicitacaoAmizade pedidoDoBrom = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.of(pedidoDoBrom));
        when(solicitacaoRepository.save(pedidoDoBrom)).thenReturn(pedidoDoBrom);

        SolicitacaoAmizadeResponseDto resposta = service.enviar(1L, 2L);

        assertEquals(5L, resposta.id());
        assertEquals(StatusSolicitacaoAmizade.ACEITA, resposta.status());
        assertEquals(AGORA, resposta.decidedAt());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveExigirEsperaParaReenviarPedidoRecusado() {
        SolicitacaoAmizade recusada = solicitacao(5L, aria, brom, StatusSolicitacaoAmizade.RECUSADA);
        recusada.setDecidedAt(AGORA.minusHours(3));
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.empty());
        when(solicitacaoRepository.findFirstByRemetenteIdAndDestinatarioIdAndStatusOrderByDecidedAtDesc(
                1L, 2L, StatusSolicitacaoAmizade.RECUSADA)).thenReturn(Optional.of(recusada));

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class, () -> service.enviar(1L, 2L));

        assertEquals("Este usuário recusou seu pedido. Você poderá enviar outro em 21 horas.", erro.getMessage());
        verify(solicitacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    void devePermitirReenviarPedidoRecusadoDepoisDaEspera() {
        SolicitacaoAmizade recusada = solicitacao(5L, aria, brom, StatusSolicitacaoAmizade.RECUSADA);
        recusada.setDecidedAt(AGORA.minus(SolicitacaoAmizade.ESPERA_APOS_RECUSA).minusMinutes(1));
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(brom));
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(aria));
        when(solicitacaoRepository.findEntre(eq(1L), eq(2L), anyCollection())).thenReturn(Optional.empty());
        when(solicitacaoRepository.findFirstByRemetenteIdAndDestinatarioIdAndStatusOrderByDecidedAtDesc(
                1L, 2L, StatusSolicitacaoAmizade.RECUSADA)).thenReturn(Optional.of(recusada));
        when(solicitacaoRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(StatusSolicitacaoAmizade.PENDENTE, service.enviar(1L, 2L).status());
    }

    @Test
    void deveAceitarPedidoRecebido() {
        SolicitacaoAmizade pedido = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(solicitacaoRepository.save(pedido)).thenReturn(pedido);

        SolicitacaoAmizadeResponseDto resposta = service.aceitar(5L, 1L);

        assertEquals(StatusSolicitacaoAmizade.ACEITA, resposta.status());
        assertEquals(AGORA, resposta.decidedAt());
    }

    @Test
    void deveRecusarPedidoRecebido() {
        SolicitacaoAmizade pedido = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(solicitacaoRepository.save(pedido)).thenReturn(pedido);

        assertEquals(StatusSolicitacaoAmizade.RECUSADA, service.recusar(5L, 1L).status());
    }

    @Test
    void deveNegarAceitarOuRecusarPedidoQueNaoFoiRecebidoPeloUsuario() {
        SolicitacaoAmizade pedido = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        assertThrows(AccessDeniedException.class, () -> service.aceitar(5L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.recusar(5L, 3L));
        assertEquals(StatusSolicitacaoAmizade.PENDENTE, pedido.getStatus());
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveCancelarPedidoEnviado() {
        SolicitacaoAmizade pedido = solicitacao(5L, aria, brom, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(solicitacaoRepository.save(pedido)).thenReturn(pedido);

        assertEquals(StatusSolicitacaoAmizade.CANCELADA, service.cancelar(5L, 1L).status());
    }

    @Test
    void deveNegarCancelarPedidoDeOutroUsuario() {
        SolicitacaoAmizade pedido = solicitacao(5L, aria, brom, StatusSolicitacaoAmizade.PENDENTE);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        assertThrows(AccessDeniedException.class, () -> service.cancelar(5L, 2L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveImpedirResponderPedidoQueJaFoiRespondido() {
        SolicitacaoAmizade pedido = solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.CANCELADA);
        when(solicitacaoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        assertThrows(RegraDeNegocioException.class, () -> service.aceitar(5L, 1L));
        verify(solicitacaoRepository, never()).save(any());
    }

    @Test
    void deveAvisarQuandoOPedidoNaoExiste() {
        when(solicitacaoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.aceitar(99L, 1L));
    }

    @Test
    void deveListarPedidosRecebidosEEnviadosPendentes() {
        when(solicitacaoRepository.findRecebidas(1L, StatusSolicitacaoAmizade.PENDENTE))
                .thenReturn(List.of(solicitacao(5L, brom, aria, StatusSolicitacaoAmizade.PENDENTE)));
        when(solicitacaoRepository.findEnviadas(1L, StatusSolicitacaoAmizade.PENDENTE))
                .thenReturn(List.of(solicitacao(6L, aria, cael, StatusSolicitacaoAmizade.PENDENTE)));

        assertEquals("brom", service.listarRecebidas(1L).get(0).remetenteUsername());
        assertEquals("cael", service.listarEnviadas(1L).get(0).destinatarioUsername());
    }

    private static Usuario usuario(Long id, String username, PapelUsuario papel) {
        Usuario usuario = new Usuario(username, username + "@arcana.test", "hash", papel);
        usuario.setId(id);
        return usuario;
    }

    private static SolicitacaoAmizade solicitacao(Long id, Usuario remetente, Usuario destinatario,
                                                  StatusSolicitacaoAmizade status) {
        SolicitacaoAmizade solicitacao = new SolicitacaoAmizade(remetente, destinatario, AGORA.minusDays(1));
        solicitacao.setId(id);
        solicitacao.setStatus(status);
        return solicitacao;
    }
}
