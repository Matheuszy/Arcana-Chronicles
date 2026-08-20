-- Mesas de jogo
CREATE TABLE IF NOT EXISTS mesas (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(50)  NOT NULL DEFAULT 'ABERTA',
    master_id    BIGINT       NOT NULL,
    master_name  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mesa_master
        FOREIGN KEY (master_id)
        REFERENCES usuarios (id)
        ON DELETE RESTRICT
);


CREATE TABLE IF NOT EXISTS mesa_participantes (
    mesa_id       BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,  
    character_id  BIGINT,               

    PRIMARY KEY (mesa_id, user_id),

    CONSTRAINT fk_participante_mesa
        FOREIGN KEY (mesa_id)
        REFERENCES mesas (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_participante_usuario
        FOREIGN KEY (user_id)
        REFERENCES usuarios (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_participante_personagem
        FOREIGN KEY (character_id)
        REFERENCES personagens (id)
        ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS mesa_npcs (
    mesa_id      BIGINT NOT NULL,
    personagem_id BIGINT NOT NULL,

    PRIMARY KEY (mesa_id, personagem_id),

    CONSTRAINT fk_mesa_npc_mesa
        FOREIGN KEY (mesa_id)
        REFERENCES mesas (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mesa_npc_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE
);
