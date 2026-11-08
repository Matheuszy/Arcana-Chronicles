package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record SecaoEquipamentoDto(
        List<PersonagemResponseDto.EquipamentoDto> equipamentos,
        List<String> items,
        String pacoteEquipamento,
        List<String> itensDoPacote,
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
                List.copyOf(p.getItens()),
                p.getPacoteEquipamento(),
                List.copyOf(p.getItensDoPacote()),
                p.getMoedasCobre(),
                p.getMoedasPrata(),
                p.getMoedasElectro(),
                p.getMoedasOuro(),
                p.getMoedasPlatina(),
                p.isMoedasRoladas());
    }
}
