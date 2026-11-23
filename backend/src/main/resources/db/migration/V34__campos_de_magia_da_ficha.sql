ALTER TABLE magias
    ADD COLUMN school        VARCHAR(30),
    ADD COLUMN casting_time  VARCHAR(60),
    ADD COLUMN spell_range   VARCHAR(60),
    ADD COLUMN components    VARCHAR(20),
    ADD COLUMN material      VARCHAR(400),
    ADD COLUMN duration      VARCHAR(60),
    ADD COLUMN concentration BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN ritual        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN higher_levels TEXT,
    ADD COLUMN classes       VARCHAR(200),
    ADD COLUMN catalog_key   VARCHAR(60);

ALTER TABLE magias
    ADD CONSTRAINT fk_magia_catalogo
        FOREIGN KEY (catalog_key)
        REFERENCES catalogo_magias (chave)
        ON DELETE SET NULL;

CREATE INDEX idx_magias_catalog_key ON magias (catalog_key);
