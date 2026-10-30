package com.arcana.backend.character.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Pericia {
    ATLETISMO("atletismo", Atributo.FORCA),
    ACROBACIA("acrobacia", Atributo.DESTREZA),
    FURTIVIDADE("furtividade", Atributo.DESTREZA),
    PRESTIDIGITACAO("prestidigitacao", Atributo.DESTREZA),
    ARCANISMO("arcanismo", Atributo.INTELIGENCIA),
    HISTORIA("historia", Atributo.INTELIGENCIA),
    INVESTIGACAO("investigacao", Atributo.INTELIGENCIA),
    NATUREZA("natureza", Atributo.INTELIGENCIA),
    RELIGIAO("religiao", Atributo.INTELIGENCIA),
    INTUICAO("intuicao", Atributo.SABEDORIA),
    LIDAR_COM_ANIMAIS("lidarComAnimais", Atributo.SABEDORIA),
    MEDICINA("medicina", Atributo.SABEDORIA),
    PERCEPCAO("percepcao", Atributo.SABEDORIA),
    SOBREVIVENCIA("sobrevivencia", Atributo.SABEDORIA),
    ATUACAO("atuacao", Atributo.CARISMA),
    ENGANACAO("enganacao", Atributo.CARISMA),
    INTIMIDACAO("intimidacao", Atributo.CARISMA),
    PERSUASAO("persuasao", Atributo.CARISMA);

    private final String chave;
    private final Atributo atributo;

    Pericia(String chave, Atributo atributo) {
        this.chave = chave;
        this.atributo = atributo;
    }

    @JsonValue
    public String getChave() {
        return chave;
    }

    public Atributo getAtributo() {
        return atributo;
    }

    @JsonCreator
    public static Pericia daChave(String chave) {
        return Arrays.stream(values())
                .filter(pericia -> pericia.chave.equals(chave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perícia desconhecida: " + chave));
    }
}
