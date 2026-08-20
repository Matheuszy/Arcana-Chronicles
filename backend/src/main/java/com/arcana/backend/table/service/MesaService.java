package com.arcana.backend.table.service;

import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaResponseDto;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.repository.MesaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
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
    public MesaResponseDto criar(MesaRequestDto dto, Long masterId) {
        Mesa mesa = new Mesa();
        mesa.setName(dto.name());
        mesa.setDescription(dto.description());
        mesa.setMasterId(masterId);
        mesa.setMasterName(dto.masterName());
        mesa.setStatus(StatusMesa.ABERTA);

        // Mestre já entra na mesa ao criar
        mesa.getParticipants().add(
                new Participante(masterId, dto.masterName(), "MESTRE", null));

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

        if (jaParticipa) {
            // Atualiza personagem escolhido
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

    /** Atualiza o status da mesa (ABERTA → EM_ANDAMENTO → ENCERRADA) */
    @Transactional
    public MesaResponseDto atualizarStatus(Long mesaId, StatusMesa novoStatus, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new RuntimeException("Apenas o mestre pode alterar o status da mesa.");
        }

        mesa.setStatus(novoStatus);
        return MesaResponseDto.from(mesaRepository.save(mesa));
    }

    /** Remove a mesa (apenas o mestre pode fazer isso) */
    @Transactional
    public void deletar(Long mesaId, Long masterId) {
        Mesa mesa = findOrThrow(mesaId);

        if (!mesa.getMasterId().equals(masterId)) {
            throw new RuntimeException("Apenas o mestre pode excluir a mesa.");
        }

        mesaRepository.delete(mesa);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Mesa findOrThrow(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada: " + id));
    }
}
