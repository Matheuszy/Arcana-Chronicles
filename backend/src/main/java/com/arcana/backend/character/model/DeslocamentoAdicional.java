package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class DeslocamentoAdicional {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDeslocamento tipo;

    @Column(nullable = false)
    private Integer metros;

    @Column(length = 60)
    private String origem;

    @Column(nullable = false)
    private boolean temporario;

    public DeslocamentoAdicional() {}

    public DeslocamentoAdicional(TipoDeslocamento tipo, Integer metros, String origem, boolean temporario) {
        this.tipo = tipo;
        this.metros = metros;
        this.origem = origem;
        this.temporario = temporario;
    }
}
