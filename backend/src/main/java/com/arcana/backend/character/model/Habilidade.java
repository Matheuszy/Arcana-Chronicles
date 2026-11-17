package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Habilidade {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    public Habilidade() {}

    public Habilidade(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
