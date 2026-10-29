package com.arcana.backend.table;

import com.arcana.backend.table.model.StatusMesa;

public record MesaStatusAlteradoEvent(Long mesaId, StatusMesa status) {}
