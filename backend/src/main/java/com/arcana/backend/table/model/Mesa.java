package com.arcana.backend.table.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusMesa status = StatusMesa.ABERTA;

    @Column(name = "master_id", nullable = false)
    private Long masterId;

    @Column(name = "master_name", nullable = false)
    private String masterName;

    /** Participantes da mesa — mapeados na tabela mesa_participantes */
    @ElementCollection
    @CollectionTable(name = "mesa_participantes", joinColumns = @JoinColumn(name = "mesa_id"))
    private List<Participante> participants = new ArrayList<>();

    /** IDs de NPCs/Monstros vinculados à mesa */
    @ElementCollection
    @CollectionTable(name = "mesa_npcs", joinColumns = @JoinColumn(name = "mesa_id"))
    @Column(name = "personagem_id")
    private List<Long> npcIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Mesa() {}
}
