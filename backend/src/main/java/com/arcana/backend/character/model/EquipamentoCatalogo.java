package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Immutable
@Getter
@AllArgsConstructor
@Table(name = "catalogo_equipamentos")
public class EquipamentoCatalogo {

    @Id
    @Column(length = 60)
    private String chave;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "nome_busca", nullable = false, length = 80)
    private String nomeBusca;

    @Column(name = "versao_regras", nullable = false, length = 20)
    private String versaoRegras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaEquipamento categoria;

    @Column(nullable = false, length = 40)
    private String subcategoria;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_pc", nullable = false)
    private Integer precoEmPc;

    @Column(name = "peso_kg", nullable = false, precision = 7, scale = 3)
    private BigDecimal pesoKg;

    @Column(length = 10)
    private String dano;

    @Column(name = "tipo_dano", length = 20)
    private String tipoDano;

    @Column(length = 200)
    private String propriedades;

    @Column(name = "classe_armadura")
    private Integer classeArmadura;

    @Column(name = "soma_destreza")
    private Boolean somaDestreza;

    @Column(name = "destreza_maxima")
    private Integer destrezaMaxima;

    @Column(name = "forca_minima")
    private Integer forcaMinima;

    @Column(name = "desvantagem_furtividade", nullable = false)
    private boolean desvantagemFurtividade;

    protected EquipamentoCatalogo() {}
}
