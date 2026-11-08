package com.arcana.backend.character.secao;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;

import java.util.List;
import java.util.Map;

public record SecaoAtributosDto(
        Integer hpMax,
        Integer hpCurrent,
        Integer vidaTemporaria,
        Integer armorClass,
        Integer caAdicional,
        int classeArmaduraTotal,
        Integer deslocamento,
        List<PersonagemResponseDto.DeslocamentoAdicionalDto> deslocamentosAdicionais,
        boolean inspiracao,
        Integer sucessosContraMorte,
        Integer falhasContraMorte,
        Integer proficiencyBonus,
        PersonagemResponseDto.AtributosDto attributes,
        Map<String, Integer> savingThrows,
        Map<String, Integer> skills,
        int percepcaoPassiva
) {
    public static SecaoAtributosDto from(Personagem p) {
        return new SecaoAtributosDto(
                p.getHpMax(),
                p.getHpCurrent(),
                p.getVidaTemporaria(),
                p.getArmorClass(),
                p.getCaAdicional(),
                p.getArmorClass() + p.getCaAdicional(),
                p.getDeslocamento(),
                PersonagemResponseDto.deslocamentosAdicionais(p),
                p.isInspiracao(),
                p.getSucessosContraMorte(),
                p.getFalhasContraMorte(),
                p.getBonusProficiencia(),
                PersonagemResponseDto.atributos(p),
                PersonagemResponseDto.salvaguardasEmOrdem(p),
                PersonagemResponseDto.periciasEmOrdem(p),
                p.calcularPercepcaoPassiva());
    }
}
