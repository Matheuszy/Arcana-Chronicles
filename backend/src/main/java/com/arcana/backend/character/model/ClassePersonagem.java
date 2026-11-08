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

    @Column(name = "dado_vida", nullable = false)
    private Integer dadoVida = DADO_VIDA_PADRAO;

    @Column(name = "dados_vida_disponiveis", nullable = false)
    private Integer dadosVidaDisponiveis;

    public static final int DADO_VIDA_PADRAO = 8;

    public ClassePersonagem() {}

    public ClassePersonagem(String classe, String subclasse, Integer nivel) {
        this(classe, subclasse, nivel, DADO_VIDA_PADRAO, nivel);
    }

    public ClassePersonagem(String classe, String subclasse, Integer nivel, Integer dadoVida,
                            Integer dadosVidaDisponiveis) {
        this.classe = classe;
        this.subclasse = subclasse;
        this.nivel = nivel;
        this.dadoVida = dadoVida;
        this.dadosVidaDisponiveis = dadosVidaDisponiveis;
    }
}
