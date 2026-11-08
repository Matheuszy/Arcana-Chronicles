CREATE TABLE personagem_idiomas (
    personagem_id BIGINT      NOT NULL,
    idioma        VARCHAR(60) NOT NULL,
    posicao       INT         NOT NULL,

    CONSTRAINT fk_idiomas_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_idiomas
        PRIMARY KEY (personagem_id, posicao)
);

CREATE TABLE personagem_proficiencias_ferramentas (
    personagem_id BIGINT      NOT NULL,
    ferramenta    VARCHAR(60) NOT NULL,
    posicao       INT         NOT NULL,

    CONSTRAINT fk_ferramentas_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_proficiencias_ferramentas
        PRIMARY KEY (personagem_id, posicao)
);

CREATE TABLE personagem_proficiencias_armas (
    personagem_id BIGINT      NOT NULL,
    arma          VARCHAR(60) NOT NULL,
    posicao       INT         NOT NULL,

    CONSTRAINT fk_armas_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_proficiencias_armas
        PRIMARY KEY (personagem_id, posicao)
);

CREATE TABLE personagem_proficiencias_armaduras (
    personagem_id BIGINT      NOT NULL,
    armadura      VARCHAR(60) NOT NULL,
    posicao       INT         NOT NULL,

    CONSTRAINT fk_armaduras_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_proficiencias_armaduras
        PRIMARY KEY (personagem_id, posicao)
);
