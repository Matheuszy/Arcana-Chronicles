package com.arcana.backend.character.model;

public enum Moeda {
    COBRE("Cobre"),
    PRATA("Prata"),
    ELECTRO("Electro"),
    OURO("Ouro"),
    PLATINA("Platina");

    private final String nome;

    Moeda(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
