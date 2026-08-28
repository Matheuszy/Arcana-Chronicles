package com.arcana.backend.table;

import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.StatusMesa;

import java.time.LocalDateTime;
import java.util.List;

public record MesaResponseDto(

        Long id,
        String name,
        String description,
        StatusMesa status,
        Long masterId,
        String masterName,
        List<ParticipanteDto> participants,
        List<Long> npcIds,
        LocalDateTime createdAt

) {
    public record ParticipanteDto(
            Long userId,
            String displayName,
            String role,
            Long characterId
    ) {}

    public static MesaResponseDto from(Mesa m) {
        List<ParticipanteDto> parts = m.getParticipants() == null ? List.of() :
                m.getParticipants().stream()
                        .map(p -> new ParticipanteDto(
                                p.getUserId(),
                                p.getDisplayName(),
                                p.getRole(),
                                p.getCharacterId()))
                        .toList();

        return new MesaResponseDto(
                m.getId(),
                m.getName(),
                m.getDescription(),
                m.getStatus(),
                m.getMasterId(),
                m.getMasterName(),
                parts,
                m.getNpcIds() != null ? m.getNpcIds() : List.of(),
                m.getCreatedAt()
        );
    }
}
