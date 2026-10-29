package com.arcana.backend.table;

public record AcessoMesaDto(
        Long mesaId,
        Long userId,
        String displayName,
        String role
) {}
