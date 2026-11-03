package com.arcana.backend.friend.dto.response;

import com.arcana.backend.friend.model.SolicitacaoAmizade;
import com.arcana.backend.friend.model.StatusSolicitacaoAmizade;

import java.time.LocalDateTime;

public record SolicitacaoAmizadeResponseDto(
        Long id,
        Long remetenteId,
        String remetenteUsername,
        Long destinatarioId,
        String destinatarioUsername,
        StatusSolicitacaoAmizade status,
        LocalDateTime createdAt,
        LocalDateTime decidedAt
) {
    public static SolicitacaoAmizadeResponseDto from(SolicitacaoAmizade s) {
        return new SolicitacaoAmizadeResponseDto(
                s.getId(),
                s.getRemetente().getId(),
                s.getRemetente().getUsername(),
                s.getDestinatario().getId(),
                s.getDestinatario().getUsername(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getDecidedAt());
    }
}
