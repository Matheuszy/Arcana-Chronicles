package com.arcana.backend.friend.service;

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
import com.arcana.backend.presence.NotificacaoDeUsuarioEvent;
import com.arcana.backend.presence.PresencaService;
import com.arcana.backend.presence.model.MensagemDoCanal;
import com.arcana.backend.user.model.CodigoAmigo;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AmizadeService {

    public static final int MINIMO_DE_CARACTERES_DA_BUSCA = 3;
    public static final int MAXIMO_DE_CARACTERES_DA_BUSCA = 100;
    public static final int LIMITE_DE_RESULTADOS = 10;

    private final UsuarioRepositorie usuarioRepositorie;
    private final SolicitacaoAmizadeRepository solicitacaoRepository;
    private final BloqueioUsuarioRepository bloqueioRepository;
    private final PresencaService presencaService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AmizadeService(UsuarioRepositorie usuarioRepositorie,
                          SolicitacaoAmizadeRepository solicitacaoRepository,
                          BloqueioUsuarioRepository bloqueioRepository,
                          PresencaService presencaService,
                          ApplicationEventPublisher eventPublisher,
                          Clock clock) {
        this.usuarioRepositorie = usuarioRepositorie;
        this.solicitacaoRepository = solicitacaoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.presencaService = presencaService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public List<UsuarioEncontradoDto> buscar(Long usuarioId, String termo) {
        String termoLimpo = validarTermoDaBusca(termo);

        Set<Long> ocultos = new HashSet<>(bloqueioRepository.findIdsBloqueadosEmQualquerSentido(usuarioId));
        ocultos.add(usuarioId);

        Map<Long, Usuario> encontrados = new LinkedHashMap<>();
        CodigoAmigo.normalizar(termoLimpo)
                .flatMap(usuarioRepositorie::findByCodigoAmigo)
                .filter(usuario -> !ocultos.contains(usuario.getId()))
                .ifPresent(usuario -> encontrados.put(usuario.getId(), usuario));
        usuarioRepositorie.findTop10ByUsernameContainingIgnoreCaseAndIdNotInOrderByUsernameAsc(termoLimpo, ocultos)
                .forEach(usuario -> encontrados.putIfAbsent(usuario.getId(), usuario));

        List<Usuario> resultado = encontrados.values().stream().limit(LIMITE_DE_RESULTADOS).toList();
        if (resultado.isEmpty()) {
            return List.of();
        }

        Map<Long, SolicitacaoAmizade> solicitacoesPorUsuario = solicitacaoRepository
                .findEntreUsuarioEOutros(usuarioId, resultado.stream().map(Usuario::getId).toList(),
                        StatusSolicitacaoAmizade.ATIVOS)
                .stream()
                .collect(Collectors.toMap(s -> s.outroUsuario(usuarioId).getId(), Function.identity(),
                        (primeira, segunda) -> primeira));

        return resultado.stream()
                .map(usuario -> paraUsuarioEncontrado(usuario, solicitacoesPorUsuario.get(usuario.getId()), usuarioId))
                .toList();
    }

    @Transactional
    public List<AmigoDto> listarAmigos(Long usuarioId) {
        return solicitacaoRepository.findAmizadesDe(usuarioId, StatusSolicitacaoAmizade.ACEITA).stream()
                .map(amizade -> {
                    Usuario amigo = amizade.outroUsuario(usuarioId);
                    return new AmigoDto(amigo.getId(), amigo.getUsername(), amigo.getPapel(),
                            presencaService.estadoDe(amigo.getId()), amizade.getDecidedAt());
                })
                .sorted(Comparator.comparing(AmigoDto::username, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public boolean saoAmigos(Long usuarioId, Long outroId) {
        return solicitacaoRepository.findEntre(usuarioId, outroId, List.of(StatusSolicitacaoAmizade.ACEITA))
                .isPresent();
    }

    @Transactional
    public SolicitacaoAmizadeResponseDto enviar(Long remetenteId, Long destinatarioId) {
        if (remetenteId.equals(destinatarioId)) {
            throw new RegraDeNegocioException("Você não pode enviar um pedido de amizade para si mesmo.");
        }

        Usuario destinatario = usuarioRepositorie.findById(destinatarioId)
                .filter(usuario -> !bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(destinatarioId, remetenteId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + destinatarioId));

        if (bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(remetenteId, destinatarioId)) {
            throw new RegraDeNegocioException(
                    "Você bloqueou este usuário. Desbloqueie para enviar um pedido de amizade.");
        }

        var existente = solicitacaoRepository.findEntre(remetenteId, destinatarioId, StatusSolicitacaoAmizade.ATIVOS);
        if (existente.isPresent()) {
            return responderAPedidoExistente(existente.get(), remetenteId);
        }

        validarEsperaAposRecusa(remetenteId, destinatarioId);

        Usuario remetente = usuarioRepositorie.findById(remetenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + remetenteId));
        SolicitacaoAmizade solicitacao = new SolicitacaoAmizade(remetente, destinatario, agora());
        SolicitacaoAmizade salva = solicitacaoRepository.saveAndFlush(solicitacao);
        notificarAlteracao(remetenteId, destinatarioId);
        return SolicitacaoAmizadeResponseDto.from(salva);
    }

    @Transactional
    public List<SolicitacaoAmizadeResponseDto> listarRecebidas(Long usuarioId) {
        return solicitacaoRepository.findRecebidas(usuarioId, StatusSolicitacaoAmizade.PENDENTE).stream()
                .map(SolicitacaoAmizadeResponseDto::from)
                .toList();
    }

    @Transactional
    public List<SolicitacaoAmizadeResponseDto> listarEnviadas(Long usuarioId) {
        return solicitacaoRepository.findEnviadas(usuarioId, StatusSolicitacaoAmizade.PENDENTE).stream()
                .map(SolicitacaoAmizadeResponseDto::from)
                .toList();
    }

    @Transactional
    public SolicitacaoAmizadeResponseDto aceitar(Long solicitacaoId, Long usuarioId) {
        SolicitacaoAmizade solicitacao = buscarSolicitacao(solicitacaoId);
        if (!solicitacao.recebidaPor(usuarioId)) {
            throw new AccessDeniedException("Apenas quem recebeu o pedido de amizade pode aceitá-lo.");
        }
        return decidir(solicitacao, StatusSolicitacaoAmizade.ACEITA);
    }

    @Transactional
    public SolicitacaoAmizadeResponseDto recusar(Long solicitacaoId, Long usuarioId) {
        SolicitacaoAmizade solicitacao = buscarSolicitacao(solicitacaoId);
        if (!solicitacao.recebidaPor(usuarioId)) {
            throw new AccessDeniedException("Apenas quem recebeu o pedido de amizade pode recusá-lo.");
        }
        return decidir(solicitacao, StatusSolicitacaoAmizade.RECUSADA);
    }

    @Transactional
    public SolicitacaoAmizadeResponseDto cancelar(Long solicitacaoId, Long usuarioId) {
        SolicitacaoAmizade solicitacao = buscarSolicitacao(solicitacaoId);
        if (!solicitacao.enviadaPor(usuarioId)) {
            throw new AccessDeniedException("Apenas quem enviou o pedido de amizade pode cancelá-lo.");
        }
        return decidir(solicitacao, StatusSolicitacaoAmizade.CANCELADA);
    }

    @Transactional
    public void removerAmizade(Long usuarioId, Long amigoId) {
        SolicitacaoAmizade amizade = solicitacaoRepository
                .findEntre(usuarioId, amigoId, List.of(StatusSolicitacaoAmizade.ACEITA))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vocês não são amigos."));

        solicitacaoRepository.delete(amizade);
        eventPublisher.publishEvent(new AmizadeDesfeitaEvent(usuarioId, amigoId));
        notificarAlteracao(usuarioId, amigoId);
    }

    @Transactional
    public UsuarioBloqueadoDto bloquear(Long bloqueadorId, Long bloqueadoId) {
        if (bloqueadorId.equals(bloqueadoId)) {
            throw new RegraDeNegocioException("Você não pode bloquear a si mesmo.");
        }
        Usuario bloqueado = usuarioRepositorie.findById(bloqueadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + bloqueadoId));
        if (bloqueioRepository.existsByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId)) {
            throw new RegraDeNegocioException("Você já bloqueou este usuário.");
        }
        Usuario bloqueador = usuarioRepositorie.findById(bloqueadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + bloqueadorId));

        solicitacaoRepository.findEntre(bloqueadorId, bloqueadoId, StatusSolicitacaoAmizade.ATIVOS)
                .ifPresent(ativa -> {
                    if (ativa.pendente()) {
                        ativa.decidir(StatusSolicitacaoAmizade.CANCELADA, agora());
                        solicitacaoRepository.save(ativa);
                    } else {
                        solicitacaoRepository.delete(ativa);
                    }
                });

        BloqueioUsuario bloqueio = bloqueioRepository.saveAndFlush(new BloqueioUsuario(bloqueador, bloqueado, agora()));
        eventPublisher.publishEvent(new AmizadeDesfeitaEvent(bloqueadorId, bloqueadoId));
        notificarAlteracao(bloqueadorId, bloqueadoId);
        return UsuarioBloqueadoDto.from(bloqueio);
    }

    @Transactional
    public void desbloquear(Long bloqueadorId, Long bloqueadoId) {
        BloqueioUsuario bloqueio = bloqueioRepository.findByBloqueadorIdAndBloqueadoId(bloqueadorId, bloqueadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este usuário não está bloqueado."));
        bloqueioRepository.delete(bloqueio);
    }

    @Transactional
    public List<UsuarioBloqueadoDto> listarBloqueados(Long bloqueadorId) {
        return bloqueioRepository.findFeitosPor(bloqueadorId).stream()
                .map(UsuarioBloqueadoDto::from)
                .toList();
    }

    private String validarTermoDaBusca(String termo) {
        String termoLimpo = termo == null ? "" : termo.trim();
        if (termoLimpo.length() < MINIMO_DE_CARACTERES_DA_BUSCA) {
            throw new IllegalArgumentException(
                    "Digite ao menos " + MINIMO_DE_CARACTERES_DA_BUSCA + " caracteres para buscar.");
        }
        if (termoLimpo.length() > MAXIMO_DE_CARACTERES_DA_BUSCA) {
            throw new IllegalArgumentException(
                    "A busca pode ter no máximo " + MAXIMO_DE_CARACTERES_DA_BUSCA + " caracteres.");
        }
        return termoLimpo;
    }

    private UsuarioEncontradoDto paraUsuarioEncontrado(Usuario usuario, SolicitacaoAmizade solicitacao, Long usuarioId) {
        if (solicitacao == null) {
            return new UsuarioEncontradoDto(usuario.getId(), usuario.getUsername(), usuario.getPapel(),
                    RelacaoDeAmizade.NENHUMA, null);
        }
        RelacaoDeAmizade relacao;
        if (!solicitacao.pendente()) {
            relacao = RelacaoDeAmizade.AMIGOS;
        } else if (solicitacao.enviadaPor(usuarioId)) {
            relacao = RelacaoDeAmizade.PEDIDO_ENVIADO;
        } else {
            relacao = RelacaoDeAmizade.PEDIDO_RECEBIDO;
        }
        return new UsuarioEncontradoDto(usuario.getId(), usuario.getUsername(), usuario.getPapel(),
                relacao, solicitacao.getId());
    }

    private SolicitacaoAmizadeResponseDto responderAPedidoExistente(SolicitacaoAmizade existente, Long remetenteId) {
        if (!existente.pendente()) {
            throw new RegraDeNegocioException("Vocês já são amigos.");
        }
        if (existente.enviadaPor(remetenteId)) {
            throw new RegraDeNegocioException("Você já enviou um pedido de amizade para este usuário.");
        }
        return decidir(existente, StatusSolicitacaoAmizade.ACEITA);
    }

    private void validarEsperaAposRecusa(Long remetenteId, Long destinatarioId) {
        solicitacaoRepository.findFirstByRemetenteIdAndDestinatarioIdAndStatusOrderByDecidedAtDesc(
                        remetenteId, destinatarioId, StatusSolicitacaoAmizade.RECUSADA)
                .map(recusada -> recusada.tempoAteNovoPedido(agora()))
                .filter(restante -> !restante.isZero())
                .ifPresent(restante -> {
                    throw new RegraDeNegocioException(
                            "Este usuário recusou seu pedido. Você poderá enviar outro em " + descrever(restante) + ".");
                });
    }

    private String descrever(Duration restante) {
        long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
        if (minutos < 60) {
            return minutos + (minutos == 1 ? " minuto" : " minutos");
        }
        long horas = (minutos + 59) / 60;
        return horas + (horas == 1 ? " hora" : " horas");
    }

    private SolicitacaoAmizade buscarSolicitacao(Long solicitacaoId) {
        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido de amizade não encontrado: " + solicitacaoId));
    }

    private SolicitacaoAmizadeResponseDto decidir(SolicitacaoAmizade solicitacao, StatusSolicitacaoAmizade decisao) {
        if (!solicitacao.pendente()) {
            throw new RegraDeNegocioException("Este pedido de amizade já foi respondido.");
        }
        solicitacao.decidir(decisao, agora());
        SolicitacaoAmizade salva = solicitacaoRepository.save(solicitacao);
        notificarAlteracao(solicitacao.getRemetente().getId(), solicitacao.getDestinatario().getId());
        return SolicitacaoAmizadeResponseDto.from(salva);
    }

    private void notificarAlteracao(Long usuarioId, Long outroUsuarioId) {
        eventPublisher.publishEvent(
                NotificacaoDeUsuarioEvent.para(MensagemDoCanal.amizadesAlteradas(), usuarioId, outroUsuarioId));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
