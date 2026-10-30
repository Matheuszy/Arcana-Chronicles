CREATE TABLE personagem_classes (
    personagem_id BIGINT      NOT NULL,
    posicao       INT         NOT NULL,
    classe        VARCHAR(60) NOT NULL,
    subclasse     VARCHAR(60),
    nivel         INT         NOT NULL,

    CONSTRAINT fk_classes_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_classes
        PRIMARY KEY (personagem_id, posicao)
);

INSERT INTO personagem_classes (personagem_id, posicao, classe, subclasse, nivel)
SELECT id,
       0,
       COALESCE(classe, subclasse),
       CASE WHEN classe IS NULL THEN NULL ELSE subclasse END,
       level
FROM personagens
WHERE classe IS NOT NULL OR subclasse IS NOT NULL;

ALTER TABLE personagens
    DROP COLUMN classe,
    DROP COLUMN subclasse,
    ADD COLUMN talentos TEXT;
