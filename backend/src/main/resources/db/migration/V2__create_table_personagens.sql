CREATE TABLE IF NOT EXISTS personagens (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    kind               VARCHAR(50)  NOT NULL,
    level              INT          NOT NULL,
    hp_max             INT          NOT NULL,
    hp_current         INT          NOT NULL,
    armor_class        INT          NOT NULL,

    
    forca              INT,
    destreza           INT,
    constituicao       INT,
    inteligencia       INT,
    sabedoria          INT,
    carisma            INT,

    
    equipment          TEXT,
    spell_slots        TEXT,
    backstory          TEXT,
    personality_prompt TEXT,
    avatar_url         VARCHAR(255),

    
    owner_id           BIGINT       NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_personagem_owner
        FOREIGN KEY (owner_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS personagem_pericias (
    personagem_id BIGINT       NOT NULL,
    pericia       VARCHAR(100) NOT NULL,

    CONSTRAINT fk_pericias_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS magias (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    level         INT          NOT NULL DEFAULT 0,
    description   TEXT,
    personagem_id BIGINT       NOT NULL,

    CONSTRAINT fk_magia_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE
);
