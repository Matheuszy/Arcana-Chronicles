ALTER TABLE personagem_equipamentos
    ADD COLUMN tipo_dano VARCHAR(20),
    ADD COLUMN peso_kg NUMERIC(7,3),
    ADD COLUMN catalogo_chave VARCHAR(60),
    ADD CONSTRAINT fk_personagem_equipamentos_catalogo
        FOREIGN KEY (catalogo_chave)
        REFERENCES catalogo_equipamentos (chave),
    ADD CONSTRAINT ck_personagem_equipamentos_peso
        CHECK (peso_kg IS NULL OR peso_kg >= 0);
