package com.arcana.backend.table.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "solicitacoes_entrada")
public class SolicitacaoEntrada {

    public static final Duration ESPERA_APOS_RECUSA = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mesa_id", nullable = false)
    private Long mesaId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "character_id")
    private Long characterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Duration tempoAteNovaSolicitacao(LocalDateTime agora) {
        if (status != StatusSolicitacao.RECUSADA || decidedAt == null) {
            return Duration.ZERO;
        }
        Duration restante = Duration.between(agora, decidedAt.plus(ESPERA_APOS_RECUSA));
        return restante.isNegative() ? Duration.ZERO : restante;
    }

    public SolicitacaoEntrada() {}

    public SolicitacaoEntrada(Long mesaId, Long userId, String displayName, Long characterId) {
        this.mesaId = mesaId;
        this.userId = userId;
        this.displayName = displayName;
        this.characterId = characterId;
    }
}
