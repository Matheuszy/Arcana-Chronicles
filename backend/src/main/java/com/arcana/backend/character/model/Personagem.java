package com.arcana.backend.character.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "personagens")
public class Personagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoJogador kind;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer hpMax;

    @Column(nullable = false)
    private Integer hpCurrent;

    @Column(nullable = false)
    private Integer armorClass;

    @Embedded
    private Atributos attributes;

    /** Perícias com proficiência — armazenadas como lista de strings */
    @ElementCollection
    @CollectionTable(name = "personagem_pericias", joinColumns = @JoinColumn(name = "personagem_id"))
    @Column(name = "pericia")
    private List<String> skills = new ArrayList<>();

    /** Equipamentos em texto livre */
    @Column(columnDefinition = "TEXT")
    private String equipment;

    /** Magias do personagem */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "personagem_id")
    private List<Magia> spells = new ArrayList<>();

    /** Slots de magia por círculo (1–9) — JSON simples como string */
    @Column(columnDefinition = "TEXT")
    private String spellSlots;

    @Column(columnDefinition = "TEXT")
    private String backstory;

    @Column(columnDefinition = "TEXT")
    private String personalityPrompt;

    @Column
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Usuario owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Personagem() {}
}
