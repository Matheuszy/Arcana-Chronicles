package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record SecaoEquipamentoDto(
        List<PersonagemResponseDto.EquipamentoDto> equipamentos,
        List<PersonagemResponseDto.ItemDto> items,
        String pacoteEquipamento,
        List<PersonagemResponseDto.ItemDto> itensDoPacote,
        Integer moedasCobre,
        Integer moedasPrata,
        Integer moedasElectro,
        Integer moedasOuro,
        Integer moedasPlatina,
        boolean moedasRoladas
) {
    public static SecaoEquipamentoDto from(Personagem p) {
        return new SecaoEquipamentoDto(
                PersonagemResponseDto.equipamentos(p),
                PersonagemResponseDto.itens(p.getItens()),
                p.getPacoteEquipamento(),
                PersonagemResponseDto.itens(p.getItensDoPacote()),
                p.getMoedasCobre(),
                p.getMoedasPrata(),
                p.getMoedasElectro(),
                p.getMoedasOuro(),
                p.getMoedasPlatina(),
                p.isMoedasRoladas());
    }
}
