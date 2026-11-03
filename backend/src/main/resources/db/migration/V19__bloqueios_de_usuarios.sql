CREATE TABLE bloqueios_usuario (
    id             BIGSERIAL  PRIMARY KEY,
    bloqueador_id  BIGINT     NOT NULL,
    bloqueado_id   BIGINT     NOT NULL,
    created_at     TIMESTAMP  NOT NULL,

    CONSTRAINT chk_bloqueio_sem_autobloqueio
        CHECK (bloqueador_id <> bloqueado_id),

    CONSTRAINT uk_bloqueio_por_par
        UNIQUE (bloqueador_id, bloqueado_id),

    CONSTRAINT fk_bloqueio_bloqueador
        FOREIGN KEY (bloqueador_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_bloqueio_bloqueado
        FOREIGN KEY (bloqueado_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE
);

CREATE INDEX ix_bloqueio_bloqueado ON bloqueios_usuario (bloqueado_id);
