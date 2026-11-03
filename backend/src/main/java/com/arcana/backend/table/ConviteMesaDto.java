package com.arcana.backend.table;

import com.arcana.backend.table.model.ConviteMesa;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.StatusConviteMesa;

import java.time.LocalDateTime;

public record ConviteMesaDto(
        Long id,
        Long mesaId,
        String mesaNome,
        Long remetenteId,
        String remetenteUsername,
        boolean convitePeloMestre,
        Long convidadoId,
        String convidadoUsername,
        StatusConviteMesa status,
        LocalDateTime createdAt
) {
    public static ConviteMesaDto from(ConviteMesa convite, Mesa mesa) {
        return new ConviteMesaDto(
                convite.getId(),
                convite.getMesaId(),
                mesa != null ? mesa.getName() : null,
                convite.getRemetente().getId(),
                convite.getRemetente().getUsername(),
                mesa != null && mesa.getMasterId().equals(convite.getRemetente().getId()),
                convite.getConvidado().getId(),
                convite.getConvidado().getUsername(),
                convite.getStatus(),
                convite.getCreatedAt());
    }
}
