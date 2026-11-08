CREATE TABLE personagem_deslocamentos_adicionais (
    personagem_id BIGINT      NOT NULL,
    posicao       INT         NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    metros        INT         NOT NULL,
    origem        VARCHAR(60),
    temporario    BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_deslocamentos_adicionais_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_deslocamentos_adicionais
        PRIMARY KEY (personagem_id, posicao)
);
