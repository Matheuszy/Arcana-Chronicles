package com.arcana.backend.character.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Column(length = 60)
    private String classe;

    @Column(length = 60)
    private String subclasse;

    @Column(name = "bonus_proficiencia", nullable = false)
    private Integer bonusProficiencia = 2;

    @Embedded
    private Atributos attributes;

    @ElementCollection
    @CollectionTable(name = "personagem_salvaguardas", joinColumns = @JoinColumn(name = "personagem_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "atributo")
    @Column(name = "bonus", nullable = false)
    private Map<Atributo, Integer> salvaguardas = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "personagem_itens", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "item")
    private List<String> itens = new ArrayList<>();


    @ElementCollection
    @CollectionTable(name = "personagem_pericias", joinColumns = @JoinColumn(name = "personagem_id"))
    @MapKeyColumn(name = "pericia")
    @Column(name = "bonus", nullable = false)
    private Map<String, Integer> skills = new LinkedHashMap<>();

 
    @Column(columnDefinition = "TEXT")
    private String equipment;

  
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "personagem_id")
    private List<Magia> spells = new ArrayList<>();


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
