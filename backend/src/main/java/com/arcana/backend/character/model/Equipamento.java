package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

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

    @Column(name = "tipo_dano", length = 20)
    private String tipoDano;

    @Column(name = "peso_kg", precision = 7, scale = 3)
    private BigDecimal pesoKg;

    @ManyToOne
    @JoinColumn(name = "catalogo_chave")
    private EquipamentoCatalogo catalogo;

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

    public boolean editadaEmRelacaoAoCatalogo() {
        if (catalogo == null) {
            return false;
        }
        boolean arma = tipo == TipoEquipamento.ARMA;
        return !(Objects.equals(nome, catalogo.getNome())
                && tipo == catalogo.tipoNaFicha()
                && Objects.equals(dano, arma ? catalogo.danoNaFicha() : null)
                && Objects.equals(tipoDano, arma ? catalogo.getTipoDano() : null)
                && Objects.equals(classeArmadura, arma ? null : catalogo.getClasseArmadura())
                && Objects.equals(propriedades, catalogo.propriedadesNaFicha())
                && pesoKg != null && pesoKg.compareTo(catalogo.getPesoKg()) == 0);
    }
}
