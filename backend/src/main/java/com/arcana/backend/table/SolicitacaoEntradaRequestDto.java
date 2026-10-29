package com.arcana.backend.table;

import jakarta.validation.constraints.NotNull;

public record SolicitacaoEntradaRequestDto(

        @NotNull(message = "Escolha o personagem que vai usar na mesa")
        Long characterId
) {}
