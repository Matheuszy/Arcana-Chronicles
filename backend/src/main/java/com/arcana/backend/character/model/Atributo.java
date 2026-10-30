package com.arcana.backend.character.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Atributo {
    FORCA("forca"),
    DESTREZA("destreza"),
    CONSTITUICAO("constituicao"),
    INTELIGENCIA("inteligencia"),
    SABEDORIA("sabedoria"),
    CARISMA("carisma");

    private final String chave;

    Atributo(String chave) {
        this.chave = chave;
    }

    @JsonValue
    public String getChave() {
        return chave;
    }

    @JsonCreator
    public static Atributo daChave(String chave) {
        return Arrays.stream(values())
                .filter(atributo -> atributo.chave.equals(chave))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Atributo desconhecido: " + chave));
    }

    public int valorEm(Atributos atributos) {
        return switch (this) {
            case FORCA -> atributos.getForca();
            case DESTREZA -> atributos.getDestreza();
            case CONSTITUICAO -> atributos.getConstituicao();
            case INTELIGENCIA -> atributos.getInteligencia();
            case SABEDORIA -> atributos.getSabedoria();
            case CARISMA -> atributos.getCarisma();
        };
    }
}
