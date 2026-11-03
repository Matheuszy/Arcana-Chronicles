CREATE TABLE convites_mesa (
    id            BIGSERIAL   PRIMARY KEY,
    mesa_id       BIGINT      NOT NULL,
    remetente_id  BIGINT      NOT NULL,
    convidado_id  BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at    TIMESTAMP   NOT NULL,
    decided_at    TIMESTAMP,

    CONSTRAINT chk_convite_mesa_status
        CHECK (status IN ('PENDENTE', 'ACEITO', 'RECUSADO', 'EXPIRADO', 'CANCELADO')),

    CONSTRAINT chk_convite_mesa_sem_autoconvite
        CHECK (remetente_id <> convidado_id),

    CONSTRAINT fk_convite_mesa_mesa
        FOREIGN KEY (mesa_id)
        REFERENCES mesas (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_convite_mesa_remetente
        FOREIGN KEY (remetente_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_convite_mesa_convidado
        FOREIGN KEY (convidado_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_convite_mesa_pendente_por_convidado
    ON convites_mesa (mesa_id, convidado_id)
    WHERE status = 'PENDENTE';

CREATE INDEX ix_convite_mesa_convidado_status ON convites_mesa (convidado_id, status);

CREATE INDEX ix_convite_mesa_mesa_status ON convites_mesa (mesa_id, status);
