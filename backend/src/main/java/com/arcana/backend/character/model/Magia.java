package com.arcana.backend.character.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    public Magia() {}

    public Magia(String name, Integer level, String description) {
        this.name = name;
        this.level = level;
        this.description = description;
    }
}
