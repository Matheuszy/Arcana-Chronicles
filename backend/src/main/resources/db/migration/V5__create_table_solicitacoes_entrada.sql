CREATE TABLE IF NOT EXISTS solicitacoes_entrada (
    id            BIGSERIAL    PRIMARY KEY,
    mesa_id       BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    character_id  BIGINT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    decided_at    TIMESTAMP,

    CONSTRAINT chk_solicitacao_status
        CHECK (status IN ('PENDENTE', 'ACEITA', 'RECUSADA')),

    CONSTRAINT fk_solicitacao_mesa
        FOREIGN KEY (mesa_id)
        REFERENCES mesas (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_solicitacao_usuario
        FOREIGN KEY (user_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_solicitacao_personagem
        FOREIGN KEY (character_id)
        REFERENCES personagens (id)
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitacao_pendente_por_usuario
    ON solicitacoes_entrada (mesa_id, user_id)
    WHERE status = 'PENDENTE';
