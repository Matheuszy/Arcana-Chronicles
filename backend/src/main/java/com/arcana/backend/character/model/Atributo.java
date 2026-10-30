package com.arcana.backend.character.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Atributo {
    FORCA("forca", "Força"),
    DESTREZA("destreza", "Destreza"),
    CONSTITUICAO("constituicao", "Constituição"),
    INTELIGENCIA("inteligencia", "Inteligência"),
    SABEDORIA("sabedoria", "Sabedoria"),
    CARISMA("carisma", "Carisma");

    private final String chave;
    private final String nome;

    Atributo(String chave, String nome) {
        this.chave = chave;
        this.nome = nome;
    }

    public String getNome() {
        return nome;
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

    public Integer valorEm(Atributos atributos) {
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
