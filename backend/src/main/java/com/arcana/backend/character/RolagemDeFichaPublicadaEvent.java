package com.arcana.backend.character;

import java.util.List;

public record RolagemDeFichaPublicadaEvent(
        Long mesaId,
        Long autorId,
        String autorNome,
        String expressao,
        int lados,
        List<Integer> dados,
        int total,
        String descricao
) {
    public RolagemDeFichaPublicadaEvent {
        dados = List.copyOf(dados);
    }
}
