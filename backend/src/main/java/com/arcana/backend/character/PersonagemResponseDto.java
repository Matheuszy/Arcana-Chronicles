package com.arcana.backend.character;

import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;

import java.time.LocalDateTime;

public record PersonagemResponseDto(

        Long id,
        String name,
        TipoJogador kind,
        Integer level,
        Integer hpMax,
        Integer hpCurrent,
        Integer armorClass,
        AtributosDto attributes,
        String backstory,
        String personalityPrompt,
        String avatarUrl,
        Long ownerId,
        LocalDateTime createdAt

) {
    public record AtributosDto(
            Integer forca,
            Integer destreza,
            Integer constituicao,
            Integer inteligencia,
            Integer sabedoria,
            Integer carisma
    ) {}

    /** Converte uma entidade Personagem para o DTO de resposta */
    public static PersonagemResponseDto from(Personagem p) {
        AtributosDto attrs = null;
        if (p.getAttributes() != null) {
            attrs = new AtributosDto(
                    p.getAttributes().getForca(),
                    p.getAttributes().getDestreza(),
                    p.getAttributes().getConstituicao(),
                    p.getAttributes().getInteligencia(),
                    p.getAttributes().getSabedoria(),
                    p.getAttributes().getCarisma()
            );
        }

        return new PersonagemResponseDto(
                p.getId(),
                p.getName(),
                p.getKind(),
                p.getLevel(),
                p.getHpMax(),
                p.getHpCurrent(),
                p.getArmorClass(),
                attrs,
                p.getBackstory(),
                p.getPersonalityPrompt(),
                p.getAvatarUrl(),
                p.getOwner() != null ? p.getOwner().getId() : null,
                p.getCreatedAt()
        );
    }
}
