package com.arcana.backend;

import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.friend.AmizadeDesfeitaEvent;
import com.arcana.backend.friend.service.AmizadeService;
import com.arcana.backend.presence.NotificacaoDeUsuarioEvent;
import com.arcana.backend.presence.PresencaService;
import com.arcana.backend.presence.UsuarioFicouOfflineEvent;
import com.arcana.backend.presence.model.TipoMensagemDoCanal;
import com.arcana.backend.table.AceiteDeConviteDto;
import com.arcana.backend.table.ConviteMesaDto;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.model.ConviteMesa;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.StatusConviteMesa;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.repository.ConviteMesaRepository;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.service.ConviteMesaService;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteMesaServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 20, 0);

    @Mock
    private ConviteMesaRepository conviteRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    @Mock
    private MesaService mesaService;

    @Mock
    private AmizadeService amizadeService;

    @Mock
    private PresencaService presencaService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ConviteMesaService service;

    private final Usuario mestre = usuario(1L, "gandalf", PapelUsuario.MESTRE);
    private final Usuario jogador = usuario(2L, "aria", PapelUsuario.JOGADOR);
    private final Usuario amigo = usuario(3L, "brom", PapelUsuario.JOGADOR);
    private final Usuario outroMestre = usuario(4L, "elrond", PapelUsuario.MESTRE);
    private Mesa mesa;

    @BeforeEach
    void setUp() {
        Clock relogio = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ConviteMesaService(conviteRepository, mesaRepository, usuarioRepositorie, mesaService,
                amizadeService, presencaService, eventPublisher, relogio);
        mesa = new Mesa();
        mesa.setId(10L);
        mesa.setName("A Torre");
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);
        mesa.getParticipants().add(new Participante(1L, "gandalf", "MESTRE", null));
        mesa.getParticipants().add(new Participante(2L, "aria", "JOGADOR", 100L));
    }

    private void prepararConviteValido(Long remetenteId, Usuario remetente) {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(3L)).thenReturn(Optional.of(amigo));
        when(usuarioRepositorie.findById(remetenteId)).thenReturn(Optional.of(remetente));
        when(amizadeService.saoAmigos(remetenteId, 3L)).thenReturn(true);
        when(presencaService.online(3L)).thenReturn(true);
        when(conviteRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ConviteMesa convite = invocation.getArgument(0);
            convite.setId(50L);
            return convite;
        });
    }

    @Test
    void deveConvidarAmigoOnlineENotificarOConvidado() {
        prepararConviteValido(1L, mestre);

        ConviteMesaDto convite = service.convidar(10L, 1L, 3L);

        assertEquals(50L, convite.id());
        assertEquals("A Torre", convite.mesaNome());
        assertTrue(convite.convitePeloMestre());
        ArgumentCaptor<NotificacaoDeUsuarioEvent> notificacao = ArgumentCaptor.forClass(NotificacaoDeUsuarioEvent.class);
        verify(eventPublisher).publishEvent(notificacao.capture());
        assertEquals(List.of(3L), notificacao.getValue().usuarioIds());
        assertEquals(TipoMensagemDoCanal.CONVITE_RECEBIDO, notificacao.getValue().mensagem().type());
    }

    @Test
    void devePermitirQueJogadorParticipanteConvide() {
        prepararConviteValido(2L, jogador);

        assertFalse(service.convidar(10L, 2L, 3L).convitePeloMestre());
    }

    @Test
    void deveNegarConviteDeQuemNaoParticipaDaMesa() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.convidar(10L, 9L, 3L));
        verify(conviteRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveExigirAmizade() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(3L)).thenReturn(Optional.of(amigo));

        assertThrows(RegraDeNegocioException.class, () -> service.convidar(10L, 1L, 3L));
        verify(conviteRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarConviteParaContaDeMestre() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(4L)).thenReturn(Optional.of(outroMestre));
        when(amizadeService.saoAmigos(1L, 4L)).thenReturn(true);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.convidar(10L, 1L, 4L));
        assertEquals("Contas de mestre não entram em mesas como jogador.", erro.getMessage());
    }

    @Test
    void deveRecusarConviteParaQuemJaParticipa() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(2L)).thenReturn(Optional.of(jogador));
        when(amizadeService.saoAmigos(1L, 2L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.convidar(10L, 1L, 2L));
    }

    @Test
    void deveRecusarConviteQuandoAMesaNaoEstaAberta() {
        mesa.setStatus(StatusMesa.EM_ANDAMENTO);
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(3L)).thenReturn(Optional.of(amigo));
        when(amizadeService.saoAmigos(1L, 3L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.convidar(10L, 1L, 3L));
    }

    @Test
    void deveRecusarConviteParaAmigoOffline() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(3L)).thenReturn(Optional.of(amigo));
        when(amizadeService.saoAmigos(1L, 3L)).thenReturn(true);
        when(presencaService.online(3L)).thenReturn(false);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.convidar(10L, 1L, 3L));
        assertEquals("Este amigo não está online.", erro.getMessage());
    }

    @Test
    void deveImpedirConviteDuplicado() {
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(usuarioRepositorie.findById(3L)).thenReturn(Optional.of(amigo));
        when(amizadeService.saoAmigos(1L, 3L)).thenReturn(true);
        when(presencaService.online(3L)).thenReturn(true);
        when(conviteRepository.existsByMesaIdAndConvidadoIdAndStatus(10L, 3L, StatusConviteMesa.PENDENTE))
                .thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.convidar(10L, 1L, 3L));
        verify(conviteRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveIncluirNaMesaAoAceitarConviteDoMestre() {
        ConviteMesa convite = convite(mestre);
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite));
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(conviteRepository.save(convite)).thenReturn(convite);

        AceiteDeConviteDto aceite = service.aceitar(50L, 3L, 300L);

        assertTrue(aceite.entrouNaMesa());
        assertEquals(StatusConviteMesa.ACEITO, convite.getStatus());
        verify(mesaService).incluirJogadorConvidadoPeloMestre(10L, 3L, "brom", 300L);
        verify(mesaService, never()).solicitarEntrada(anyLong(), anyLong(), any(), any(), anyLong());
    }

    @Test
    void deveCriarSolicitacaoParaOMestreAoAceitarConviteDeJogador() {
        ConviteMesa convite = convite(jogador);
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite));
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(conviteRepository.save(convite)).thenReturn(convite);

        AceiteDeConviteDto aceite = service.aceitar(50L, 3L, 300L);

        assertFalse(aceite.entrouNaMesa());
        verify(mesaService).solicitarEntrada(10L, 3L, "brom", PapelUsuario.JOGADOR, 300L);
        verify(mesaService, never()).incluirJogadorConvidadoPeloMestre(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    void deveNegarResponderConviteDeOutroUsuario() {
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite(mestre)));

        assertThrows(AccessDeniedException.class, () -> service.aceitar(50L, 2L, 300L));
        assertThrows(AccessDeniedException.class, () -> service.recusar(50L, 2L));
        verifyNoInteractions(mesaService);
    }

    @Test
    void deveRecusarAceiteQuandoAMesaDeixouDeEstarAberta() {
        mesa.setStatus(StatusMesa.PAUSADA);
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite(mestre)));
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class, () -> service.aceitar(50L, 3L, 300L));
        verifyNoInteractions(mesaService);
    }

    @Test
    void deveRecusarConvite() {
        ConviteMesa convite = convite(mestre);
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite));
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(conviteRepository.save(convite)).thenReturn(convite);

        assertEquals(StatusConviteMesa.RECUSADO, service.recusar(50L, 3L).status());
    }

    @Test
    void deveImpedirResponderConviteQueNaoEstaMaisPendente() {
        ConviteMesa convite = convite(mestre);
        convite.setStatus(StatusConviteMesa.EXPIRADO);
        when(conviteRepository.findById(50L)).thenReturn(Optional.of(convite));

        assertThrows(RegraDeNegocioException.class, () -> service.recusar(50L, 3L));
    }

    @Test
    void deveExpirarConvitesQuandoAMesaDeixaDeEstarAberta() {
        ConviteMesa convite = convite(mestre);
        when(conviteRepository.findByMesaIdAndStatus(10L, StatusConviteMesa.PENDENTE)).thenReturn(List.of(convite));

        service.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(10L, StatusMesa.EM_ANDAMENTO));

        assertEquals(StatusConviteMesa.EXPIRADO, convite.getStatus());
        verify(conviteRepository).save(convite);
        verify(eventPublisher).publishEvent(any(NotificacaoDeUsuarioEvent.class));
    }

    @Test
    void naoDeveExpirarConvitesQuandoAMesaVoltaAFicarAberta() {
        service.aoAlterarStatusDaMesa(new MesaStatusAlteradoEvent(10L, StatusMesa.ABERTA));

        verifyNoInteractions(conviteRepository);
    }

    @Test
    void deveExpirarConvitesQuandoOConvidadoFicaOffline() {
        ConviteMesa convite = convite(mestre);
        when(conviteRepository.findByConvidadoIdAndStatus(3L, StatusConviteMesa.PENDENTE)).thenReturn(List.of(convite));

        service.aoFicarOffline(new UsuarioFicouOfflineEvent(3L));

        assertEquals(StatusConviteMesa.EXPIRADO, convite.getStatus());
    }

    @Test
    void deveCancelarConvitesEntreUsuariosQueDesfizeramAAmizade() {
        ConviteMesa convite = convite(mestre);
        when(conviteRepository.findEntre(1L, 3L, StatusConviteMesa.PENDENTE)).thenReturn(List.of(convite));

        service.aoDesfazerAmizade(new AmizadeDesfeitaEvent(1L, 3L));

        assertEquals(StatusConviteMesa.CANCELADO, convite.getStatus());
    }

    private ConviteMesa convite(Usuario remetente) {
        ConviteMesa convite = new ConviteMesa(10L, remetente, amigo, AGORA.minusMinutes(1));
        convite.setId(50L);
        return convite;
    }

    private static Usuario usuario(Long id, String username, PapelUsuario papel) {
        Usuario usuario = new Usuario(username, username + "@arcana.test", "hash", papel);
        usuario.setId(id);
        return usuario;
    }
}
