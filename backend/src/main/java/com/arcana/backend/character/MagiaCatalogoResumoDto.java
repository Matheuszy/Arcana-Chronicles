package com.arcana.backend.character;

import com.arcana.backend.character.model.MagiaCatalogo;

import java.util.List;

public record MagiaCatalogoResumoDto(
        String chave,
        String nome,
        Integer nivel,
        String escola,
        List<String> classes
) {
    public static MagiaCatalogoResumoDto from(MagiaCatalogo magia) {
        return new MagiaCatalogoResumoDto(magia.getChave(), magia.getNome(), magia.getNivel(), magia.getEscola(),
                List.copyOf(magia.getClasses()));
    }
}
