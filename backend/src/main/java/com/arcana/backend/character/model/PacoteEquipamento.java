package com.arcana.backend.character.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "pacotes_equipamento")
public class PacoteEquipamento {

    @Id
    @Column(length = 30)
    private String chave;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    private Integer posicao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pacote_equipamento_itens", joinColumns = @JoinColumn(name = "pacote_chave"))
    @OrderColumn(name = "posicao")
    @Column(name = "item", nullable = false, length = 100)
    private List<String> itens = new ArrayList<>();

    protected PacoteEquipamento() {}

    public PacoteEquipamento(String chave, String nome, Integer posicao, List<String> itens) {
        this.chave = chave;
        this.nome = nome;
        this.posicao = posicao;
        this.itens = new ArrayList<>(itens);
    }
}
