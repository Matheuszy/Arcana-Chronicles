package com.arcana.backend.character.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "magias")
public class Magia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    private String school;

    @Column(name = "casting_time", length = 60)
    private String castingTime;

    @Column(name = "spell_range", length = 60)
    private String range;

    @Column(length = 20)
    private String components;

    @Column(length = 400)
    private String material;

    @Column(length = 60)
    private String duration;

    @Column(nullable = false)
    private boolean concentration;

    @Column(nullable = false)
    private boolean ritual;

    @Column(name = "higher_levels", columnDefinition = "TEXT")
    private String higherLevels;

    @Column(length = 200)
    private String classes;

    @ManyToOne
    @JoinColumn(name = "catalog_key")
    private MagiaCatalogo catalogo;

    public Magia() {}

    public Magia(String name, Integer level, String description) {
        this.name = name;
        this.level = level;
        this.description = description;
    }

    public boolean editadaEmRelacaoAoCatalogo() {
        if (catalogo == null) {
            return false;
        }
        return !(Objects.equals(name, catalogo.getNome())
                && Objects.equals(level, catalogo.getNivel())
                && iguais(description, catalogo.getDescricao())
                && iguais(school, catalogo.getEscola())
                && iguais(castingTime, catalogo.getTempoConjuracao())
                && iguais(range, catalogo.getAlcance())
                && iguais(components, catalogo.getComponentes())
                && iguais(material, catalogo.getMaterial())
                && iguais(duration, catalogo.getDuracao())
                && concentration == catalogo.isConcentracao()
                && ritual == catalogo.isRitual()
                && iguais(higherLevels, catalogo.getNiveisSuperiores())
                && iguais(classes, String.join(", ", catalogo.getClasses())));
    }

    private static boolean iguais(String daFicha, String doCatalogo) {
        return Objects.equals(daFicha == null ? "" : daFicha, doCatalogo == null ? "" : doCatalogo);
    }
}
