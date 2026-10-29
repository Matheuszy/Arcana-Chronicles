package com.arcana.backend.table.service;

import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaResponseDto;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.repository.MesaRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final PersonagemRepository personagemRepository;

    public MesaService(MesaRepository mesaRepository, PersonagemRepository personagemRepository) {
        this.mesaRepository = mesaRepository;
        this.personagemRepository = personagemRepository;
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
    public MesaResponseDto criar(MesaRequestDto dto, Long masterId, String masterName) {
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

    /**
     * Jogador entra na mesa — endpoint POST /api/tables/{id}/entrar
     * Evita duplicatas: se o usuário já estiver na mesa, apenas atualiza o personagem.
     */
    @Transactional
    public MesaResponseDto entrar(Long mesaId, Long userId, String displayName, Long characterId) {
        Mesa mesa = findOrThrow(mesaId);

        boolean jaParticipa = mesa.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        if (!jaParticipa && mesa.getStatus() != StatusMesa.ABERTA) {
            throw new RegraDeNegocioException("A mesa não está aberta para novos jogadores.");
        }

        if (characterId != null) {
            validarPersonagemProprioEJogavel(characterId, userId);
        }

        if (jaParticipa) {
            mesa.getParticipants().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .ifPresent(p -> p.setCharacterId(characterId));
        } else {
            mesa.getParticipants().add(
                    new Participante(userId, displayName, "JOGADOR", characterId));
        }

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
