CREATE TABLE personagem_habilidades_classe (
    personagem_id BIGINT        NOT NULL,
    posicao       INT           NOT NULL,
    nome          VARCHAR(100)  NOT NULL,
    descricao     VARCHAR(1000),

    CONSTRAINT fk_habilidades_classe_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_habilidades_classe
        PRIMARY KEY (personagem_id, posicao)
);

CREATE TABLE personagem_habilidades_raca (
    personagem_id BIGINT        NOT NULL,
    posicao       INT           NOT NULL,
    nome          VARCHAR(100)  NOT NULL,
    descricao     VARCHAR(1000),

    CONSTRAINT fk_habilidades_raca_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_habilidades_raca
        PRIMARY KEY (personagem_id, posicao)
);

CREATE TABLE personagem_talentos (
    personagem_id BIGINT        NOT NULL,
    posicao       INT           NOT NULL,
    nome          VARCHAR(100)  NOT NULL,
    descricao     VARCHAR(1000),

    CONSTRAINT fk_talentos_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_talentos
        PRIMARY KEY (personagem_id, posicao)
);

INSERT INTO personagem_habilidades_classe (personagem_id, posicao, nome, descricao)
SELECT p.id,
       parte.numero - 1,
       CASE WHEN parte.total = 1 THEN 'Habilidades' ELSE 'Habilidades (parte ' || parte.numero || ')' END,
       SUBSTRING(BTRIM(p.habilidades) FROM (parte.numero - 1) * 1000 + 1 FOR 1000)
FROM personagens p
CROSS JOIN LATERAL (
    SELECT numero, CEIL(CHAR_LENGTH(BTRIM(p.habilidades)) / 1000.0)::INT AS total
    FROM generate_series(1, CEIL(CHAR_LENGTH(BTRIM(p.habilidades)) / 1000.0)::INT) AS numero
) parte
WHERE p.habilidades IS NOT NULL
  AND BTRIM(p.habilidades) <> '';

INSERT INTO personagem_habilidades_raca (personagem_id, posicao, nome, descricao)
SELECT p.id,
       parte.numero - 1,
       CASE WHEN parte.total = 1 THEN 'Habilidades de raça' ELSE 'Habilidades de raça (parte ' || parte.numero || ')' END,
       SUBSTRING(BTRIM(p.habilidades_raca) FROM (parte.numero - 1) * 1000 + 1 FOR 1000)
FROM personagens p
CROSS JOIN LATERAL (
    SELECT numero, CEIL(CHAR_LENGTH(BTRIM(p.habilidades_raca)) / 1000.0)::INT AS total
    FROM generate_series(1, CEIL(CHAR_LENGTH(BTRIM(p.habilidades_raca)) / 1000.0)::INT) AS numero
) parte
WHERE p.habilidades_raca IS NOT NULL
  AND BTRIM(p.habilidades_raca) <> '';

INSERT INTO personagem_talentos (personagem_id, posicao, nome, descricao)
SELECT p.id,
       parte.numero - 1,
       CASE WHEN parte.total = 1 THEN 'Talentos' ELSE 'Talentos (parte ' || parte.numero || ')' END,
       SUBSTRING(BTRIM(p.talentos) FROM (parte.numero - 1) * 1000 + 1 FOR 1000)
FROM personagens p
CROSS JOIN LATERAL (
    SELECT numero, CEIL(CHAR_LENGTH(BTRIM(p.talentos)) / 1000.0)::INT AS total
    FROM generate_series(1, CEIL(CHAR_LENGTH(BTRIM(p.talentos)) / 1000.0)::INT) AS numero
) parte
WHERE p.talentos IS NOT NULL
  AND BTRIM(p.talentos) <> '';

ALTER TABLE personagens
    DROP COLUMN habilidades,
    DROP COLUMN habilidades_raca,
    DROP COLUMN talentos;
