package com.arcana.backend.character;

import com.arcana.backend.character.model.PacoteEquipamento;

import java.util.List;

public record PacoteEquipamentoDto(
        String chave,
        String nome,
        List<String> itens
) {
    public static PacoteEquipamentoDto from(PacoteEquipamento pacote) {
        return new PacoteEquipamentoDto(pacote.getChave(), pacote.getNome(), List.copyOf(pacote.getItens()));
    }
}
