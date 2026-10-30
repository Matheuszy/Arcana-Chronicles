package com.arcana.backend.character;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PersonagemResponseDto(

        Long id,
        String name,
        TipoJogador kind,
        Integer level,
        Integer hpMax,
        Integer hpCurrent,
        Integer armorClass,
        AtributosDto attributes,
        Map<String, Integer> skills,
        String equipment,
        List<SpellDto> spells,
        Map<String, Integer> spellSlots,
        String backstory,
        String personalityPrompt,
        String avatarUrl,
        Long ownerId,
        LocalDateTime createdAt,
        String classe,
        String subclasse,
        Integer proficiencyBonus,
        Map<String, Integer> savingThrows,
        List<String> items

) {
    public record AtributosDto(
            Integer forca,
            Integer destreza,
            Integer constituicao,
            Integer inteligencia,
            Integer sabedoria,
            Integer carisma
    ) {}

    public record SpellDto(
            Long id,
            String name,
            Integer level,
            String description
    ) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        List<SpellDto> spellDtos = p.getSpells() == null ? Collections.emptyList() :
                p.getSpells().stream()
                        .map(m -> new SpellDto(m.getId(), m.getName(), m.getLevel(), m.getDescription()))
                        .toList();

        Map<String, Integer> slots = Collections.emptyMap();
        if (p.getSpellSlots() != null && !p.getSpellSlots().isBlank()) {
            try {
                slots = MAPPER.readValue(p.getSpellSlots(), new TypeReference<>() {});
            } catch (Exception ignored) {}
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
                periciasEmOrdem(p),
                p.getEquipment(),
                spellDtos,
                slots,
                p.getBackstory(),
                p.getPersonalityPrompt(),
                p.getAvatarUrl(),
                p.getOwner() != null ? p.getOwner().getId() : null,
                p.getCreatedAt(),
                p.getClasse(),
                p.getSubclasse(),
                p.getBonusProficiencia(),
                salvaguardasEmOrdem(p),
                List.copyOf(p.getItens())
        );
    }

    private static Map<String, Integer> periciasEmOrdem(Personagem p) {
        Map<String, Integer> pericias = new LinkedHashMap<>();
        Arrays.stream(Pericia.values())
                .filter(pericia -> p.getSkills().containsKey(pericia.getChave()))
                .forEach(pericia -> pericias.put(pericia.getChave(), p.getSkills().get(pericia.getChave())));
        return pericias;
    }

    private static Map<String, Integer> salvaguardasEmOrdem(Personagem p) {
        Map<String, Integer> salvaguardas = new LinkedHashMap<>();
        Arrays.stream(Atributo.values())
                .filter(p.getSalvaguardas()::containsKey)
                .forEach(atributo -> salvaguardas.put(atributo.getChave(), p.getSalvaguardas().get(atributo)));
        return salvaguardas;
    }
}
