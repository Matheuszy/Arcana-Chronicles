package com.arcana.backend.table.service;

import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.friend.AmizadeDesfeitaEvent;
import com.arcana.backend.friend.service.AmizadeService;
import com.arcana.backend.presence.NotificacaoDeUsuarioEvent;
import com.arcana.backend.presence.PresencaService;
import com.arcana.backend.presence.UsuarioFicouOfflineEvent;
import com.arcana.backend.presence.model.MensagemDoCanal;
import com.arcana.backend.presence.model.TipoMensagemDoCanal;
import com.arcana.backend.table.AceiteDeConviteDto;
import com.arcana.backend.table.ConviteMesaDto;
import com.arcana.backend.table.MesaStatusAlteradoEvent;
import com.arcana.backend.table.model.ConviteMesa;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.StatusConviteMesa;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.repository.ConviteMesaRepository;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConviteMesaService {

    public record ConviteEncerrado(Long conviteId, Long mesaId, StatusConviteMesa status) {}

    private final ConviteMesaRepository conviteRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepositorie usuarioRepositorie;
    private final MesaService mesaService;
    private final AmizadeService amizadeService;
    private final PresencaService presencaService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ConviteMesaService(ConviteMesaRepository conviteRepository,
                              MesaRepository mesaRepository,
                              UsuarioRepositorie usuarioRepositorie,
                              MesaService mesaService,
                              AmizadeService amizadeService,
                              PresencaService presencaService,
                              ApplicationEventPublisher eventPublisher,
                              Clock clock) {
        this.conviteRepository = conviteRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepositorie = usuarioRepositorie;
        this.mesaService = mesaService;
        this.amizadeService = amizadeService;
        this.presencaService = presencaService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ConviteMesaDto convidar(Long mesaId, Long remetenteId, Long convidadoId) {
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mesa não encontrada: " + mesaId));

        if (!participaDaMesa(mesa, remetenteId)) {
            throw new AccessDeniedException("Apenas o mestre e os participantes da mesa podem convidar amigos.");
        }
        if (remetenteId.equals(convidadoId)) {
            throw new RegraDeNegocioException("Você não pode convidar a si mesmo.");
        }

        Usuario convidado = usuarioRepositorie.findById(convidadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + convidadoId));

        if (!amizadeService.saoAmigos(remetenteId, convidadoId)) {
            throw new RegraDeNegocioException("Você só pode convidar amigos para a mesa.");
        }
        if (convidado.getPapel() != PapelUsuario.JOGADOR) {
            throw new RegraDeNegocioException("Contas de mestre não entram em mesas como jogador.");
        }
        if (participaDaMesa(mesa, convidadoId)) {
            throw new RegraDeNegocioException("Este jogador já participa da mesa.");
        }
        if (mesa.getStatus() != StatusMesa.ABERTA) {
            throw new RegraDeNegocioException("A mesa não está aberta para novos jogadores.");
        }
        if (!presencaService.online(convidadoId)) {
            throw new RegraDeNegocioException("Este amigo não está online.");
        }
        if (conviteRepository.existsByMesaIdAndConvidadoIdAndStatus(mesaId, convidadoId, StatusConviteMesa.PENDENTE)) {
            throw new RegraDeNegocioException("Este jogador já tem um convite pendente para esta mesa.");
        }

        Usuario remetente = usuarioRepositorie.findById(remetenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + remetenteId));
        ConviteMesa convite = conviteRepository.saveAndFlush(new ConviteMesa(mesaId, remetente, convidado, agora()));
        ConviteMesaDto dto = ConviteMesaDto.from(convite, mesa);
        eventPublisher.publishEvent(NotificacaoDeUsuarioEvent.para(
                new MensagemDoCanal(TipoMensagemDoCanal.CONVITE_RECEBIDO, dto), convidadoId));
        return dto;
    }

    @Transactional
    public List<ConviteMesaDto> listarPendentes(Long usuarioId) {
        List<ConviteMesa> convites = conviteRepository.findRecebidos(usuarioId, StatusConviteMesa.PENDENTE);
        Map<Long, Mesa> mesas = mesaRepository.findAllById(convites.stream().map(ConviteMesa::getMesaId).toList())
                .stream()
                .collect(Collectors.toMap(Mesa::getId, Function.identity()));
        return convites.stream()
                .map(convite -> ConviteMesaDto.from(convite, mesas.get(convite.getMesaId())))
                .toList();
    }

    @Transactional
    public AceiteDeConviteDto aceitar(Long conviteId, Long usuarioId, Long characterId) {
        ConviteMesa convite = buscarConviteDoConvidado(conviteId, usuarioId);
        Mesa mesa = mesaRepository.findById(convite.getMesaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mesa não encontrada: " + convite.getMesaId()));

        if (mesa.getStatus() != StatusMesa.ABERTA) {
            throw new RegraDeNegocioException("A mesa não está mais aberta para novos jogadores.");
        }

        Usuario convidado = convite.getConvidado();
        boolean convitePeloMestre = mesa.getMasterId().equals(convite.getRemetente().getId());
        if (convitePeloMestre) {
            mesaService.incluirJogadorConvidadoPeloMestre(
                    mesa.getId(), convidado.getId(), convidado.getUsername(), characterId);
        } else {
            mesaService.solicitarEntrada(
                    mesa.getId(), convidado.getId(), convidado.getUsername(), convidado.getPapel(), characterId);
        }

        convite.decidir(StatusConviteMesa.ACEITO, agora());
        return new AceiteDeConviteDto(ConviteMesaDto.from(conviteRepository.save(convite), mesa), convitePeloMestre);
    }

    @Transactional
    public ConviteMesaDto recusar(Long conviteId, Long usuarioId) {
        ConviteMesa convite = buscarConviteDoConvidado(conviteId, usuarioId);
        convite.decidir(StatusConviteMesa.RECUSADO, agora());
        Mesa mesa = mesaRepository.findById(convite.getMesaId()).orElse(null);
        return ConviteMesaDto.from(conviteRepository.save(convite), mesa);
    }

    @EventListener
    @Transactional
    public void aoAlterarStatusDaMesa(MesaStatusAlteradoEvent evento) {
        if (evento.status() != StatusMesa.ABERTA) {
            encerrar(conviteRepository.findByMesaIdAndStatus(evento.mesaId(), StatusConviteMesa.PENDENTE),
                    StatusConviteMesa.EXPIRADO);
        }
    }

    @EventListener
    @Transactional
    public void aoFicarOffline(UsuarioFicouOfflineEvent evento) {
        encerrar(conviteRepository.findByConvidadoIdAndStatus(evento.usuarioId(), StatusConviteMesa.PENDENTE),
                StatusConviteMesa.EXPIRADO);
    }

    @EventListener
    @Transactional
    public void aoDesfazerAmizade(AmizadeDesfeitaEvent evento) {
        encerrar(conviteRepository.findEntre(evento.usuarioId(), evento.outroUsuarioId(), StatusConviteMesa.PENDENTE),
                StatusConviteMesa.CANCELADO);
    }

    private void encerrar(List<ConviteMesa> convites, StatusConviteMesa status) {
        LocalDateTime agora = agora();
        for (ConviteMesa convite : convites) {
            convite.decidir(status, agora);
            conviteRepository.save(convite);
            eventPublisher.publishEvent(NotificacaoDeUsuarioEvent.para(
                    new MensagemDoCanal(TipoMensagemDoCanal.CONVITE_ENCERRADO,
                            new ConviteEncerrado(convite.getId(), convite.getMesaId(), status)),
                    convite.getConvidado().getId()));
        }
    }

    private ConviteMesa buscarConviteDoConvidado(Long conviteId, Long usuarioId) {
        ConviteMesa convite = conviteRepository.findById(conviteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado: " + conviteId));
        if (!convite.destinadoA(usuarioId)) {
            throw new AccessDeniedException("Apenas o convidado pode responder a este convite.");
        }
        if (!convite.pendente()) {
            throw new RegraDeNegocioException("Este convite não está mais disponível.");
        }
        return convite;
    }

    private boolean participaDaMesa(Mesa mesa, Long usuarioId) {
        return mesa.getMasterId().equals(usuarioId)
                || mesa.getParticipants().stream().anyMatch(p -> p.getUserId().equals(usuarioId));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
