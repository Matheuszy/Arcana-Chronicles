package com.arcana.backend.friend.model;

import com.arcana.backend.user.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bloqueios_usuario")
public class BloqueioUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloqueador_id", nullable = false, updatable = false)
    private Usuario bloqueador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bloqueado_id", nullable = false, updatable = false)
    private Usuario bloqueado;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BloqueioUsuario() {}

    public BloqueioUsuario(Usuario bloqueador, Usuario bloqueado, LocalDateTime createdAt) {
        this.bloqueador = bloqueador;
        this.bloqueado = bloqueado;
        this.createdAt = createdAt;
    }
}
