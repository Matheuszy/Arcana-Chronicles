package com.arcana.backend.character.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ficha_rolagens")
public class RolagemDeFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personagem_id", nullable = false)
    private Personagem personagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRolagemFicha tipo;

    @Column(nullable = false, length = 120)
    private String descricao;

    @Column(nullable = false, length = 40)
    private String expressao;

    @Column(nullable = false, length = 200)
    private String resultados;

    @Column(nullable = false)
    private Integer total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RolagemDeFicha() {}

    public RolagemDeFicha(Personagem personagem, TipoRolagemFicha tipo, String descricao, String expressao,
                          String resultados, Integer total, LocalDateTime createdAt) {
        this.personagem = personagem;
        this.tipo = tipo;
        this.descricao = descricao;
        this.expressao = expressao;
        this.resultados = resultados;
        this.total = total;
        this.createdAt = createdAt;
    }
}
