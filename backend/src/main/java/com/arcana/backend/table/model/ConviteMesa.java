package com.arcana.backend.table.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "convites_mesa")
public class ConviteMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mesa_id", nullable = false, updatable = false)
    private Long mesaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "remetente_id", nullable = false, updatable = false)
    private Usuario remetente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convidado_id", nullable = false, updatable = false)
    private Usuario convidado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConviteMesa status = StatusConviteMesa.PENDENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public ConviteMesa() {}

    public ConviteMesa(Long mesaId, Usuario remetente, Usuario convidado, LocalDateTime createdAt) {
        this.mesaId = mesaId;
        this.remetente = remetente;
        this.convidado = convidado;
        this.createdAt = createdAt;
    }

    public boolean pendente() {
        return status == StatusConviteMesa.PENDENTE;
    }

    public boolean destinadoA(Long usuarioId) {
        return convidado.getId().equals(usuarioId);
    }

    public void decidir(StatusConviteMesa decisao, LocalDateTime agora) {
        this.status = decisao;
        this.decidedAt = agora;
    }
}
