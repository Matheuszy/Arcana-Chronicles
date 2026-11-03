CREATE TABLE solicitacoes_amizade (
    id               BIGSERIAL   PRIMARY KEY,
    remetente_id     BIGINT      NOT NULL,
    destinatario_id  BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at       TIMESTAMP   NOT NULL,
    decided_at       TIMESTAMP,

    CONSTRAINT chk_solicitacao_amizade_status
        CHECK (status IN ('PENDENTE', 'ACEITA', 'RECUSADA', 'CANCELADA')),

    CONSTRAINT chk_solicitacao_amizade_sem_autoamizade
        CHECK (remetente_id <> destinatario_id),

    CONSTRAINT fk_solicitacao_amizade_remetente
        FOREIGN KEY (remetente_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_solicitacao_amizade_destinatario
        FOREIGN KEY (destinatario_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_solicitacao_amizade_ativa_por_par
    ON solicitacoes_amizade (LEAST(remetente_id, destinatario_id), GREATEST(remetente_id, destinatario_id))
    WHERE status IN ('PENDENTE', 'ACEITA');

CREATE INDEX ix_solicitacao_amizade_destinatario_status
    ON solicitacoes_amizade (destinatario_id, status);

CREATE INDEX ix_solicitacao_amizade_remetente_status
    ON solicitacoes_amizade (remetente_id, status);
