package com.arcana.backend.friend.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "solicitacoes_amizade")
public class SolicitacaoAmizade {

    public static final Duration ESPERA_APOS_RECUSA = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false, updatable = false)
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false, updatable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacaoAmizade status = StatusSolicitacaoAmizade.PENDENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public SolicitacaoAmizade() {}

    public SolicitacaoAmizade(Usuario remetente, Usuario destinatario, LocalDateTime createdAt) {
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.createdAt = createdAt;
    }

    public boolean enviadaPor(Long usuarioId) {
        return remetente.getId().equals(usuarioId);
    }

    public boolean recebidaPor(Long usuarioId) {
        return destinatario.getId().equals(usuarioId);
    }

    public boolean pendente() {
        return status == StatusSolicitacaoAmizade.PENDENTE;
    }

    public Usuario outroUsuario(Long usuarioId) {
        return enviadaPor(usuarioId) ? destinatario : remetente;
    }

    public void decidir(StatusSolicitacaoAmizade decisao, LocalDateTime agora) {
        this.status = decisao;
        this.decidedAt = agora;
    }

    public Duration tempoAteNovoPedido(LocalDateTime agora) {
        if (status != StatusSolicitacaoAmizade.RECUSADA || decidedAt == null) {
            return Duration.ZERO;
        }
        Duration restante = Duration.between(agora, decidedAt.plus(ESPERA_APOS_RECUSA));
        return restante.isNegative() ? Duration.ZERO : restante;
    }
}
