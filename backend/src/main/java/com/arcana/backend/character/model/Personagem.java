package com.arcana.backend.character.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(columnDefinition = "TEXT")
    private String backstory;

    @Column(columnDefinition = "TEXT")
    private String personalityPrompt;

    @Column
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario ownerId;

    @Column(nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = java.time.LocalDateTime.now();
    }

    public Personagem() {}

    public Personagem(String name, TipoJogador kind, Integer level,
                      Integer hpMax, Integer hpCurrent, Integer armorClass,
                      Atributos attributes, String backstory,
                      String personalityPrompt, String avatarUrl, Usuario ownerId) {
        this.name = name;
        this.kind = kind;
        this.level = level;
        this.hpMax = hpMax;
        this.hpCurrent = hpCurrent;
        this.armorClass = armorClass;
        this.attributes = attributes;
        this.backstory = backstory;
        this.personalityPrompt = personalityPrompt;
        this.avatarUrl = avatarUrl;
        this.ownerId = ownerId;
    }
}
