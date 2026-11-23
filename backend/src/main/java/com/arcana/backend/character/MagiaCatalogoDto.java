package com.arcana.backend.character;

import com.arcana.backend.character.model.MagiaCatalogo;

import java.util.List;

public record MagiaCatalogoDto(
        String chave,
        String nome,
        String versaoRegras,
        Integer nivel,
        String escola,
        String tempoConjuracao,
        String alcance,
        String componentes,
        String material,
        String duracao,
        boolean concentracao,
        boolean ritual,
        String descricao,
        String niveisSuperiores,
        List<String> classes
) {
    public static MagiaCatalogoDto from(MagiaCatalogo magia) {
        return new MagiaCatalogoDto(magia.getChave(), magia.getNome(), magia.getVersaoRegras(), magia.getNivel(),
                magia.getEscola(), magia.getTempoConjuracao(), magia.getAlcance(), magia.getComponentes(),
                magia.getMaterial(), magia.getDuracao(), magia.isConcentracao(), magia.isRitual(),
                magia.getDescricao(), magia.getNiveisSuperiores(), List.copyOf(magia.getClasses()));
    }
}
