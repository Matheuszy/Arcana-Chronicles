package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ClassePersonagem {

    @Column(nullable = false, length = 60)
    private String classe;

    @Column(length = 60)
    private String subclasse;

    @Column(nullable = false)
    private Integer nivel;

    public ClassePersonagem() {}

    public ClassePersonagem(String classe, String subclasse, Integer nivel) {
        this.classe = classe;
        this.subclasse = subclasse;
        this.nivel = nivel;
    }
}
