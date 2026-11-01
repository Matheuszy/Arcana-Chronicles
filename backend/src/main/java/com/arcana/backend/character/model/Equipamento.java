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
public class Equipamento {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEquipamento tipo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 10)
    private String dano;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Atributo atributo;

    @Column(name = "classe_armadura")
    private Integer classeArmadura;

    @Column(length = 200)
    private String propriedades;

    public Equipamento() {}

    public Equipamento(TipoEquipamento tipo, String nome, String dano, Atributo atributo,
                       Integer classeArmadura, String propriedades) {
        this.tipo = tipo;
        this.nome = nome;
        this.dano = dano;
        this.atributo = atributo;
        this.classeArmadura = classeArmadura;
        this.propriedades = propriedades;
    }
}
