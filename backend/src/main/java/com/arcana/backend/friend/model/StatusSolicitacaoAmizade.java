package com.arcana.backend.friend.model;

import java.util.List;

public enum StatusSolicitacaoAmizade {
    PENDENTE,
    ACEITA,
    RECUSADA,
    CANCELADA;

    public static final List<StatusSolicitacaoAmizade> ATIVOS = List.of(PENDENTE, ACEITA);
}
