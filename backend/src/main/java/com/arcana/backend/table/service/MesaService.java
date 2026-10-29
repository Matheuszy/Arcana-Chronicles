package com.arcana.backend.table.service;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaResponseDto;
import com.arcana.backend.table.SolicitacaoEntradaResponseDto;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.SolicitacaoEntrada;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.model.StatusSolicitacao;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.repository.SolicitacaoEntradaRepository;
import com.arcana.backend.user.model.PapelUsuario;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final PersonagemRepository personagemRepository;
    private final SolicitacaoEntradaRepository solicitacaoRepository;
    private final Clock clock;

    public MesaService(MesaRepository mesaRepository,
                       PersonagemRepository personagemRepository,
                       SolicitacaoEntradaRepository solicitacaoRepository,
                       Clock clock) {
        this.mesaRepository = mesaRepository;
        this.personagemRepository = personagemRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.clock = clock;
    }

    /** Lista todas as mesas (frontend usa GET /api/tables sem filtro) */
    public List<MesaResponseDto> listarTodas() {
        return mesaRepository.findAll()
                .stream()
                .map(MesaResponseDto::from)
                .toList();
    }

    /** Busca mesa por ID */
    public MesaResponseDto buscarPorId(Long id) {
        Mesa mesa = findOrThrow(id);
        return MesaResponseDto.from(mesa);
    }

    /** Cria uma nova mesa — o mestre entra automaticamente como participante */
    @Transactional
    public MesaResponseDto criar(MesaRequestDto dto, Long masterId, String masterName, PapelUsuario papel) {
        if (papel != PapelUsuario.MESTRE) {
            throw new AccessDeniedException("Apenas contas de mestre podem criar mesas.");
        }

        Mesa mesa = new Mesa();
        mesa.setName(dto.name());
        mesa.setDescription(dto.description());
        mesa.setMasterId(masterId);
        mesa.setMasterName(masterName);
        mesa.setStatus(StatusMesa.ABERTA);

        // Mestre já entra na mesa ao criar
        mesa.getParticipants().add(
                new Participante(masterId, masterName, "MESTRE", null));

        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    @Transactional
    public SolicitacaoEntradaResponseDto solicitarEntrada(
            Long mesaId, Long userId, String displayName, PapelUsuario papel, Long characterId) {

        if (papel != PapelUsuario.JOGADOR) {
            throw new AccessDeniedException("Contas de mestre não entram em mesas como jogador.");
        }

        Mesa mesa = findOrThrow(mesaId);

        if (participaDaMesa(mesa, userId)) {
            throw new RegraDeNegocioException("Você já participa desta mesa.");
        }

        if (mesa.getStatus() != StatusMesa.ABERTA) {
            throw new RegraDeNegocioException("A mesa não está aberta para novos jogadores.");
        }

        if (solicitacaoRepository.existsByMesaIdAndUserIdAndStatus(mesaId, userId, StatusSolicitacao.PENDENTE)) {
            throw new RegraDeNegocioException("Você já tem uma solicitação pendente nesta mesa.");
        }

        validarEsperaAposRecusa(mesaId, userId);
        validarPersonagemProprioEJogavel(characterId, userId);

        SolicitacaoEntrada solicitacao = new SolicitacaoEntrada(mesaId, userId, displayName, characterId);
        return paraResposta(solicitacaoRepository.save(solicitacao));
    }

    public SolicitacaoEntradaResponseDto buscarMinhaSolicitacao(Long mesaId, Long userId) {
        findOrThrow(mesaId);
        return solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(mesaId, userId)
                .map(this::paraResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma solicitação de entrada nesta mesa."));
    }

    public List<SolicitacaoEntradaResponseDto> listarSolicitacoesPendentes(Long mesaId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);
        validarMestreDaMesa(mesa, masterId, "Apenas o mestre pode ver as solicitações de entrada.");

        return solicitacaoRepository.findByMesaIdAndStatusOrderByCreatedAtAsc(mesaId, StatusSolicitacao.PENDENTE)
                .stream()
                .map(this::paraResposta)
                .toList();
    }

    @Transactional
    public SolicitacaoEntradaResponseDto aceitarSolicitacao(Long mesaId, Long solicitacaoId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);
        validarMestreDaMesa(mesa, masterId, "Apenas o mestre pode aceitar solicitações de entrada.");
        SolicitacaoEntrada solicitacao = buscarSolicitacaoPendenteDaMesa(mesaId, solicitacaoId);

        if (mesa.getStatus() != StatusMesa.ABERTA) {
            throw new RegraDeNegocioException("A mesa não está aberta para novos jogadores.");
        }

        Long characterId = solicitacao.getCharacterId();
        if (characterId != null) {
            validarPersonagemProprioEJogavel(characterId, solicitacao.getUserId());
        }

        if (!participaDaMesa(mesa, solicitacao.getUserId())) {
            mesa.getParticipants().add(new Participante(
                    solicitacao.getUserId(), solicitacao.getDisplayName(), "JOGADOR", characterId));
            mesaRepository.save(mesa);
        }

        return decidir(solicitacao, StatusSolicitacao.ACEITA);
    }

    @Transactional
    public SolicitacaoEntradaResponseDto recusarSolicitacao(Long mesaId, Long solicitacaoId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);
        validarMestreDaMesa(mesa, masterId, "Apenas o mestre pode recusar solicitações de entrada.");
        SolicitacaoEntrada solicitacao = buscarSolicitacaoPendenteDaMesa(mesaId, solicitacaoId);

        return decidir(solicitacao, StatusSolicitacao.RECUSADA);
    }

    public PersonagemResponseDto buscarFichaDeParticipante(Long mesaId, Long characterId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);
        validarMestreDaMesa(mesa, masterId, "Apenas o mestre da mesa pode ver fichas de outros participantes.");

        Participante participante = mesa.getParticipants().stream()
                .filter(p -> characterId.equals(p.getCharacterId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhum participante desta mesa usa o personagem " + characterId + "."));

        Personagem personagem = personagemRepository.findById(characterId)
                .filter(p -> p.getOwner().getId().equals(participante.getUserId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + characterId));

        return PersonagemResponseDto.from(personagem);
    }

    @Transactional
    public MesaResponseDto trocarPersonagem(Long mesaId, Long userId, Long characterId) {
        Mesa mesa = findOrThrow(mesaId);

        Participante participante = mesa.getParticipants().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Você ainda não participa desta mesa. Envie uma solicitação de entrada."));

        if (characterId != null) {
            validarPersonagemProprioEJogavel(characterId, userId);
        }

        participante.setCharacterId(characterId);
        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Jogador sai da mesa — o mestre não pode sair pelo mesmo endpoint */
    @Transactional
    public MesaResponseDto sair(Long mesaId, Long userId) {
        Mesa mesa = findOrThrow(mesaId);

        if (mesa.getMasterId().equals(userId)) {
            throw new RegraDeNegocioException("O mestre não pode sair da própria mesa.");
        }

        mesa.getParticipants().removeIf(p -> p.getUserId().equals(userId));
        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Atualiza o status da mesa (ABERTA → EM_ANDAMENTO → ENCERRADA) */
    @Transactional
    public MesaResponseDto atualizarStatus(Long mesaId, StatusMesa novoStatus, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new AccessDeniedException("Apenas o mestre pode alterar o status da mesa.");
        }

        mesa.setStatus(novoStatus);
        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Associa um NPC (não-jogador) à mesa — apenas o mestre, e apenas NPCs próprios */
    @Transactional
    public MesaResponseDto adicionarNpc(Long mesaId, Long npcId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new AccessDeniedException("Apenas o mestre pode gerenciar NPCs da mesa.");
        }

        validarNpcProprioDoMestre(npcId, masterId);

        if (!mesa.getNpcIds().contains(npcId)) {
            mesa.getNpcIds().add(npcId);
        }

        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Remove um NPC da mesa — apenas o mestre */
    @Transactional
    public MesaResponseDto removerNpc(Long mesaId, Long npcId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new AccessDeniedException("Apenas o mestre pode gerenciar NPCs da mesa.");
        }

        mesa.getNpcIds().remove(npcId);
        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Remove a mesa (apenas o mestre pode fazer isso) */
    @Transactional
    public void deletar(Long mesaId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new AccessDeniedException("Apenas o mestre pode excluir a mesa.");
        }

        mesaRepository.delete(mesa);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Mesa findOrThrow(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Mesa não encontrada: " + id));
    }

    private void validarMestreDaMesa(Mesa mesa, Long userId, String mensagem) {
        if (!mesa.getMasterId().equals(userId)) {
            throw new AccessDeniedException(mensagem);
        }
    }

    private SolicitacaoEntrada buscarSolicitacaoPendenteDaMesa(Long mesaId, Long solicitacaoId) {
        SolicitacaoEntrada solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .filter(s -> s.getMesaId().equals(mesaId))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitação não encontrada nesta mesa: " + solicitacaoId));

        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraDeNegocioException("Esta solicitação já foi decidida.");
        }

        return solicitacao;
    }

    private SolicitacaoEntradaResponseDto decidir(SolicitacaoEntrada solicitacao, StatusSolicitacao decisao) {
        solicitacao.setStatus(decisao);
        solicitacao.setDecidedAt(agora());
        return paraResposta(solicitacaoRepository.save(solicitacao));
    }

    private void validarEsperaAposRecusa(Long mesaId, Long userId) {
        solicitacaoRepository.findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(mesaId, userId)
                .map(ultima -> ultima.tempoAteNovaSolicitacao(agora()))
                .filter(restante -> !restante.isZero())
                .ifPresent(restante -> {
                    long minutos = Math.max(1, (restante.toSeconds() + 59) / 60);
                    throw new RegraDeNegocioException(
                            "Sua solicitação foi recusada. Você poderá solicitar novamente em "
                                    + minutos + (minutos == 1 ? " minuto." : " minutos."));
                });
    }

    private SolicitacaoEntradaResponseDto paraResposta(SolicitacaoEntrada solicitacao) {
        return SolicitacaoEntradaResponseDto.from(solicitacao, agora());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }

    private boolean participaDaMesa(Mesa mesa, Long userId) {
        return mesa.getParticipants().stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    private void validarPersonagemProprioEJogavel(Long characterId, Long userId) {
        Personagem personagem = personagemRepository.findById(characterId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + characterId));

        if (!personagem.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Personagem não pertence a este usuário.");
        }

        if (personagem.getKind() != TipoJogador.PLAYER) {
            throw new IllegalArgumentException("Apenas personagens do tipo jogador podem entrar na mesa.");
        }
    }

    private void validarNpcProprioDoMestre(Long npcId, Long masterId) {
        Personagem personagem = personagemRepository.findById(npcId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + npcId));

        if (!personagem.getOwner().getId().equals(masterId)) {
            throw new AccessDeniedException("NPC não pertence a este mestre.");
        }

        if (personagem.getKind() == TipoJogador.PLAYER) {
            throw new IllegalArgumentException("Apenas NPCs podem ser associados à mesa como NPC.");
        }
    }
}
