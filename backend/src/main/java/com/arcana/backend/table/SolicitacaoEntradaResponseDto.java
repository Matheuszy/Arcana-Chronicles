package com.arcana.backend.table;

import com.arcana.backend.table.model.SolicitacaoEntrada;
import com.arcana.backend.table.model.StatusSolicitacao;

import java.time.LocalDateTime;

public record SolicitacaoEntradaResponseDto(
        Long id,
        Long mesaId,
        Long userId,
        String displayName,
        Long characterId,
        StatusSolicitacao status,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        long segundosAteNovaSolicitacao
) {
    public static SolicitacaoEntradaResponseDto from(SolicitacaoEntrada s, LocalDateTime agora) {
        return new SolicitacaoEntradaResponseDto(
                s.getId(),
                s.getMesaId(),
                s.getUserId(),
                s.getDisplayName(),
                s.getCharacterId(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getDecidedAt(),
                (long) Math.ceil(s.tempoAteNovaSolicitacao(agora).toMillis() / 1000.0)
        );
    }
}
