package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Embeddable
@Getter
@Setter
public class ItemDaFicha {

    @Column(name = "item", nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer quantidade = 1;

    @Column(name = "peso_kg", precision = 8, scale = 4)
    private BigDecimal pesoKg;

    @ManyToOne
    @JoinColumn(name = "catalogo_chave")
    private EquipamentoCatalogo catalogo;

    public ItemDaFicha() {}

    public ItemDaFicha(String nome, int quantidade, BigDecimal pesoKg, EquipamentoCatalogo catalogo) {
        this.nome = nome;
        this.quantidade = quantidade;
        this.pesoKg = pesoKg;
        this.catalogo = catalogo;
    }

    public ItemDaFicha(String nome) {
        this(nome, 1, null, null);
    }

    public ItemDaFicha copia() {
        return new ItemDaFicha(nome, quantidade, pesoKg, catalogo);
    }

    public boolean mesmoItemQue(ItemDaFicha outro) {
        return nome.equals(outro.nome) && quantidade.equals(outro.quantidade);
    }

    public boolean editadoEmRelacaoAoCatalogo() {
        if (catalogo == null) {
            return false;
        }
        return !(Objects.equals(nome, catalogo.getNome())
                && pesoKg != null && pesoKg.compareTo(catalogo.pesoPorUnidadeKg()) == 0);
    }
}
