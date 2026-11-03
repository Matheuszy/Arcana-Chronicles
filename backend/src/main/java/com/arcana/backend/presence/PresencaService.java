package com.arcana.backend.presence;

import com.arcana.backend.chat.sessao.UsuariosEmMesaAlteradosEvent;
import com.arcana.backend.friend.model.StatusSolicitacaoAmizade;
import com.arcana.backend.friend.repository.SolicitacaoAmizadeRepository;
import com.arcana.backend.presence.model.EstadoPresenca;
import com.arcana.backend.presence.model.MensagemDoCanal;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresencaService {

    private final CanalDoUsuario canal;
    private final SessoesAtivasDeMesa sessoesAtivas;
    private final SolicitacaoAmizadeRepository solicitacaoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler agendador;
    private final Clock clock;
    private final Duration toleranciaOffline;
    private final Map<Long, EstadoPresenca> estadosAnunciados = new ConcurrentHashMap<>();

    public PresencaService(CanalDoUsuario canal,
                           SessoesAtivasDeMesa sessoesAtivas,
                           SolicitacaoAmizadeRepository solicitacaoRepository,
                           ApplicationEventPublisher eventPublisher,
                           @Qualifier("chatTaskScheduler") TaskScheduler agendador,
                           Clock clock,
                           @Value("${arcana.presenca.tolerancia-offline:10s}") Duration toleranciaOffline) {
        this.canal = canal;
        this.sessoesAtivas = sessoesAtivas;
        this.solicitacaoRepository = solicitacaoRepository;
        this.eventPublisher = eventPublisher;
        this.agendador = agendador;
        this.clock = clock;
        this.toleranciaOffline = toleranciaOffline;
    }

    public EstadoPresenca estadoDe(Long usuarioId) {
        if (sessoesAtivas.usuarioEmSessao(usuarioId)) {
            return EstadoPresenca.EM_MESA;
        }
        return canal.conectado(usuarioId) ? EstadoPresenca.ONLINE : EstadoPresenca.OFFLINE;
    }

    public boolean online(Long usuarioId) {
        return estadoDe(usuarioId) != EstadoPresenca.OFFLINE;
    }

    public void aoConectar(Long usuarioId) {
        anunciarSeMudou(usuarioId);
    }

    public void aoDesconectar(Long usuarioId) {
        if (canal.conectado(usuarioId)) {
            return;
        }
        if (toleranciaOffline.isZero() || toleranciaOffline.isNegative()) {
            anunciarSeMudou(usuarioId);
            return;
        }
        agendador.schedule(() -> anunciarSeMudou(usuarioId), clock.instant().plus(toleranciaOffline));
    }

    @EventListener
    public void aoAlterarUsuariosEmMesa(UsuariosEmMesaAlteradosEvent evento) {
        evento.usuarioIds().forEach(this::anunciarSeMudou);
    }

    public void anunciarSeMudou(Long usuarioId) {
        EstadoPresenca atual = estadoDe(usuarioId);
        EstadoPresenca anterior = atual == EstadoPresenca.OFFLINE
                ? estadosAnunciados.remove(usuarioId)
                : estadosAnunciados.put(usuarioId, atual);
        EstadoPresenca anunciadoAntes = anterior == null ? EstadoPresenca.OFFLINE : anterior;
        if (anunciadoAntes == atual) {
            return;
        }

        canal.enviar(solicitacaoRepository.findIdsDeAmigos(usuarioId, StatusSolicitacaoAmizade.ACEITA),
                MensagemDoCanal.presenca(usuarioId, atual));
        if (atual == EstadoPresenca.OFFLINE) {
            eventPublisher.publishEvent(new UsuarioFicouOfflineEvent(usuarioId));
        }
    }
}
