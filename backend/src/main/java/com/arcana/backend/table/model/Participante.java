package com.arcana.backend.table.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Participante {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** 'MESTRE' ou 'JOGADOR' */
    @Column(nullable = false)
    private String role;

    /** ID do personagem escolhido — pode ser null */
    @Column(name = "character_id")
    private Long characterId;

    public Participante() {}

    public Participante(Long userId, String displayName, String role, Long characterId) {
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.characterId = characterId;
    }
}
