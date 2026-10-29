package com.arcana.backend.table;

import com.arcana.backend.table.model.StatusMesa;

public record AcessoMesaDto(Long mesaId, Long userId, String displayName, String role, StatusMesa statusMesa) {}
