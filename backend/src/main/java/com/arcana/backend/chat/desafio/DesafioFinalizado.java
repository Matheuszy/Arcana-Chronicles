package com.arcana.backend.chat.desafio;

import java.util.Map;

public record DesafioFinalizado(
        Map<Long, DesafioDto> envolvidos,
        DesafioDto espectadores
) {

    public DesafioDto para(Long userId) {
        return envolvidos.getOrDefault(userId, espectadores);
    }
}
