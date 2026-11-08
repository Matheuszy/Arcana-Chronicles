package com.arcana.backend.character;

import com.arcana.backend.character.model.RolagemDeFicha;
import com.arcana.backend.character.model.TipoRolagemFicha;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record RolagemDeFichaDto(
        Long id,
        TipoRolagemFicha tipo,
        String descricao,
        String expressao,
        List<Integer> resultados,
        Integer total,
        LocalDateTime createdAt
) {
    public static RolagemDeFichaDto from(RolagemDeFicha rolagem) {
        List<Integer> resultados = Arrays.stream(rolagem.getResultados().split(","))
                .filter(valor -> !valor.isBlank())
                .map(Integer::valueOf)
                .toList();
        return new RolagemDeFichaDto(rolagem.getId(), rolagem.getTipo(), rolagem.getDescricao(),
                rolagem.getExpressao(), resultados, rolagem.getTotal(), rolagem.getCreatedAt());
    }
}
