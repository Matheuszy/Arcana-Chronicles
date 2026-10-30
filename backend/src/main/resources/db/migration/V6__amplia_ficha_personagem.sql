ALTER TABLE personagens
    ADD COLUMN classe             VARCHAR(60),
    ADD COLUMN subclasse          VARCHAR(60),
    ADD COLUMN bonus_proficiencia INT NOT NULL DEFAULT 2;

CREATE TABLE IF NOT EXISTS personagem_salvaguardas (
    personagem_id BIGINT      NOT NULL,
    atributo      VARCHAR(20) NOT NULL,

    CONSTRAINT fk_salvaguardas_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS personagem_itens (
    personagem_id BIGINT       NOT NULL,
    item          VARCHAR(100) NOT NULL,
    posicao       INT          NOT NULL,

    CONSTRAINT fk_itens_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_itens
        PRIMARY KEY (personagem_id, posicao)
);
