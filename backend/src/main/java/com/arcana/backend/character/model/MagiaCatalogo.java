package com.arcana.backend.character.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Immutable
@Getter
@AllArgsConstructor
@Table(name = "catalogo_magias")
public class MagiaCatalogo {

    @Id
    @Column(length = 60)
    private String chave;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(name = "nome_busca", nullable = false, length = 60)
    private String nomeBusca;

    @Column(name = "versao_regras", nullable = false, length = 20)
    private String versaoRegras;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false, length = 30)
    private String escola;

    @Column(name = "tempo_conjuracao", nullable = false, length = 60)
    private String tempoConjuracao;

    @Column(nullable = false, length = 60)
    private String alcance;

    @Column(nullable = false, length = 10)
    private String componentes;

    @Column(length = 400)
    private String material;

    @Column(nullable = false, length = 60)
    private String duracao;

    @Column(nullable = false)
    private boolean concentracao;

    @Column(nullable = false)
    private boolean ritual;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "niveis_superiores", columnDefinition = "TEXT")
    private String niveisSuperiores;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "catalogo_magia_classes", joinColumns = @JoinColumn(name = "magia_chave"))
    @Column(name = "classe", nullable = false, length = 30)
    @OrderBy
    private List<String> classes = new ArrayList<>();

    protected MagiaCatalogo() {}
}
