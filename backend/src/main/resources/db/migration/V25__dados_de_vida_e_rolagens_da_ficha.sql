ALTER TABLE personagem_classes
    ADD COLUMN dado_vida              INT NOT NULL DEFAULT 8,
    ADD COLUMN dados_vida_disponiveis INT;

UPDATE personagem_classes
SET dados_vida_disponiveis = nivel;

ALTER TABLE personagem_classes
    ALTER COLUMN dados_vida_disponiveis SET NOT NULL;

CREATE TABLE ficha_rolagens (
    id            BIGSERIAL    PRIMARY KEY,
    personagem_id BIGINT       NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    descricao     VARCHAR(120) NOT NULL,
    expressao     VARCHAR(40)  NOT NULL,
    resultados    VARCHAR(200) NOT NULL,
    total         INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT fk_ficha_rolagens_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_ficha_rolagens_personagem ON ficha_rolagens (personagem_id, created_at DESC);
