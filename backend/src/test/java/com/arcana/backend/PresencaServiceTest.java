package com.arcana.backend;

import com.arcana.backend.chat.sessao.UsuariosEmMesaAlteradosEvent;
import com.arcana.backend.friend.model.StatusSolicitacaoAmizade;
import com.arcana.backend.friend.repository.SolicitacaoAmizadeRepository;
import com.arcana.backend.presence.CanalDoUsuario;
import com.arcana.backend.presence.PresencaService;
import com.arcana.backend.presence.UsuarioFicouOfflineEvent;
import com.arcana.backend.presence.model.EstadoPresenca;
import com.arcana.backend.presence.model.MensagemDoCanal;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresencaServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CanalDoUsuario canal;

    @Mock
    private SessoesAtivasDeMesa sessoesAtivas;

    @Mock
    private SolicitacaoAmizadeRepository solicitacaoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TaskScheduler agendador;

    private PresencaService servico(Duration tolerancia) {
        return new PresencaService(canal, sessoesAtivas, solicitacaoRepository, eventPublisher, agendador,
                RELOGIO, tolerancia);
    }

    @Test
    void deveIndicarEmMesaOnlineOuOffline() {
        PresencaService service = servico(Duration.ZERO);
        when(sessoesAtivas.usuarioEmSessao(1L)).thenReturn(true);
        when(canal.conectado(2L)).thenReturn(true);

        assertEquals(EstadoPresenca.EM_MESA, service.estadoDe(1L));
        assertEquals(EstadoPresenca.ONLINE, service.estadoDe(2L));
        assertEquals(EstadoPresenca.OFFLINE, service.estadoDe(3L));
    }

    @Test
    void deveAnunciarAosAmigosQuandoOUsuarioFicaOnlineApenasUmaVez() {
        PresencaService service = servico(Duration.ZERO);
        when(canal.conectado(1L)).thenReturn(true);
        when(solicitacaoRepository.findIdsDeAmigos(1L, StatusSolicitacaoAmizade.ACEITA)).thenReturn(List.of(2L, 3L));

        service.aoConectar(1L);
        service.aoConectar(1L);

        verify(canal, times(1)).enviar(List.of(2L, 3L), MensagemDoCanal.presenca(1L, EstadoPresenca.ONLINE));
    }

    @Test
    void deveAnunciarOfflineEExpirarConvitesQuandoAUltimaConexaoFecha() {
        PresencaService service = servico(Duration.ZERO);
        when(canal.conectado(1L)).thenReturn(true, false, false);
        when(solicitacaoRepository.findIdsDeAmigos(1L, StatusSolicitacaoAmizade.ACEITA)).thenReturn(List.of(2L));

        service.aoConectar(1L);
        service.aoDesconectar(1L);

        verify(canal).enviar(List.of(2L), MensagemDoCanal.presenca(1L, EstadoPresenca.OFFLINE));
        verify(eventPublisher).publishEvent(new UsuarioFicouOfflineEvent(1L));
    }

    @Test
    void deveEsperarATolerânciaAntesDeAnunciarOffline() {
        PresencaService service = servico(Duration.ofSeconds(10));
        when(canal.conectado(1L)).thenReturn(false);

        service.aoDesconectar(1L);

        verify(agendador).schedule(any(Runnable.class), eq(RELOGIO.instant().plusSeconds(10)));
        verify(canal, never()).enviar(anyCollection(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void naoDeveAnunciarDesconexaoEnquantoOutraAbaContinuaAberta() {
        PresencaService service = servico(Duration.ZERO);
        when(canal.conectado(1L)).thenReturn(true);

        service.aoDesconectar(1L);

        verifyNoInteractions(agendador, eventPublisher);
        verify(canal, never()).enviar(anyCollection(), any());
    }

    @Test
    void deveAnunciarEmMesaQuandoOUsuarioEntraNaSalaDaMesa() {
        PresencaService service = servico(Duration.ZERO);
        when(sessoesAtivas.usuarioEmSessao(1L)).thenReturn(true);
        when(solicitacaoRepository.findIdsDeAmigos(1L, StatusSolicitacaoAmizade.ACEITA)).thenReturn(List.of(4L));

        service.aoAlterarUsuariosEmMesa(new UsuariosEmMesaAlteradosEvent(List.of(1L)));

        verify(canal).enviar(List.of(4L), MensagemDoCanal.presenca(1L, EstadoPresenca.EM_MESA));
    }
}
