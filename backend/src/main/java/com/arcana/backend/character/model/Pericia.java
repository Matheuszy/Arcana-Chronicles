package com.arcana.backend.character.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Pericia {
    ATLETISMO("atletismo", "Atletismo", Atributo.FORCA),
    ACROBACIA("acrobacia", "Acrobacia", Atributo.DESTREZA),
    FURTIVIDADE("furtividade", "Furtividade", Atributo.DESTREZA),
    PRESTIDIGITACAO("prestidigitacao", "Prestidigitação", Atributo.DESTREZA),
    ARCANISMO("arcanismo", "Arcanismo", Atributo.INTELIGENCIA),
    HISTORIA("historia", "História", Atributo.INTELIGENCIA),
    INVESTIGACAO("investigacao", "Investigação", Atributo.INTELIGENCIA),
    NATUREZA("natureza", "Natureza", Atributo.INTELIGENCIA),
    RELIGIAO("religiao", "Religião", Atributo.INTELIGENCIA),
    INTUICAO("intuicao", "Intuição", Atributo.SABEDORIA),
    LIDAR_COM_ANIMAIS("lidarComAnimais", "Lidar com Animais", Atributo.SABEDORIA),
    MEDICINA("medicina", "Medicina", Atributo.SABEDORIA),
    PERCEPCAO("percepcao", "Percepção", Atributo.SABEDORIA),
    SOBREVIVENCIA("sobrevivencia", "Sobrevivência", Atributo.SABEDORIA),
    ATUACAO("atuacao", "Atuação", Atributo.CARISMA),
    ENGANACAO("enganacao", "Enganação", Atributo.CARISMA),
    INTIMIDACAO("intimidacao", "Intimidação", Atributo.CARISMA),
    PERSUASAO("persuasao", "Persuasão", Atributo.CARISMA);

    private final String chave;
    private final String nome;
    private final Atributo atributo;

    Pericia(String chave, String nome, Atributo atributo) {
        this.chave = chave;
        this.nome = nome;
        this.atributo = atributo;
    }

    @JsonValue
    public String getChave() {
        return chave;
    }

    public String getNome() {
        return nome;
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
