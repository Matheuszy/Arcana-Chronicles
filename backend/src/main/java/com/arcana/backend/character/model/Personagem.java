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

    @Version
    private Long version;

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

    @Column(name = "vida_temporaria")
    private Integer vidaTemporaria;

    @Column(nullable = false)
    private Integer deslocamento = 9;

    @Column(nullable = false)
    private boolean inspiracao;

    @Column(name = "sucessos_contra_morte", nullable = false)
    private Integer sucessosContraMorte = 0;

    @Column(name = "falhas_contra_morte", nullable = false)
    private Integer falhasContraMorte = 0;

    @ElementCollection
    @CollectionTable(name = "personagem_classes", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    private List<ClassePersonagem> classes = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String habilidades;

    @Column(columnDefinition = "TEXT")
    private String talentos;

    @Column(length = 60)
    private String raca;

    @Column(length = 60)
    private String alinhamento;

    @Column(name = "habilidades_raca", columnDefinition = "TEXT")
    private String habilidadesRaca;

    @Column(length = 60)
    private String antecedente;

    @Column(name = "tracos_personalidade", columnDefinition = "TEXT")
    private String tracosPersonalidade;

    @Column(columnDefinition = "TEXT")
    private String ideais;

    @Column(columnDefinition = "TEXT")
    private String vinculos;

    @Column(columnDefinition = "TEXT")
    private String defeitos;

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
    @CollectionTable(name = "personagem_idiomas", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "idioma", length = 60, nullable = false)
    private List<String> idiomas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personagem_proficiencias_ferramentas", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "ferramenta", length = 60, nullable = false)
    private List<String> proficienciasFerramentas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personagem_proficiencias_armas", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "arma", length = 60, nullable = false)
    private List<String> proficienciasArmas = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personagem_proficiencias_armaduras", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "armadura", length = 60, nullable = false)
    private List<String> proficienciasArmaduras = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "personagem_pericias", joinColumns = @JoinColumn(name = "personagem_id"))
    @MapKeyColumn(name = "pericia")
    @Column(name = "bonus", nullable = false)
    private Map<String, Integer> skills = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "personagem_equipamentos", joinColumns = @JoinColumn(name = "personagem_id"))
    @OrderColumn(name = "posicao")
    private List<Equipamento> equipamentos = new ArrayList<>();

  
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "personagem_id", nullable = false)
    private List<Magia> spells = new ArrayList<>();


    @Column(columnDefinition = "TEXT")
    private String spellSlots;

    @Column(columnDefinition = "TEXT")
    private String spellSlotsCurrent;

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

    public int calcularPercepcaoPassiva() {
        int sabedoria = attributes != null && attributes.getSabedoria() != null ? attributes.getSabedoria() : 10;
        int proficiencia = bonusProficiencia != null ? bonusProficiencia : 0;
        return 10 + Math.floorDiv(sabedoria - 10, 2) + proficiencia;
    }
}
