CREATE TABLE personagem_equipamentos (
    personagem_id   BIGINT       NOT NULL,
    posicao         INT          NOT NULL,
    tipo            VARCHAR(20)  NOT NULL,
    nome            VARCHAR(100) NOT NULL,
    dano            VARCHAR(10),
    atributo        VARCHAR(20),
    classe_armadura INT,
    propriedades    VARCHAR(200),

    CONSTRAINT fk_equipamentos_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_equipamentos
        PRIMARY KEY (personagem_id, posicao)
);

INSERT INTO personagem_itens (personagem_id, posicao, item)
SELECT linhas.personagem_id,
       linhas.inicio + ROW_NUMBER() OVER (PARTITION BY linhas.personagem_id ORDER BY linhas.ordem) - 1,
       linhas.texto
FROM (
    SELECT p.id AS personagem_id,
           COALESCE((SELECT MAX(i.posicao) + 1 FROM personagem_itens i WHERE i.personagem_id = p.id), 0) AS inicio,
           parte.ordem,
           LEFT(TRIM(parte.texto), 100) AS texto
    FROM personagens p
    CROSS JOIN LATERAL regexp_split_to_table(p.equipment, E'\\r?\\n') WITH ORDINALITY AS parte(texto, ordem)
    WHERE p.equipment IS NOT NULL
) linhas
WHERE linhas.texto <> '';

ALTER TABLE personagens
    DROP COLUMN equipment;
