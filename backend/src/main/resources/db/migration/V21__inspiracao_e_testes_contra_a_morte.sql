ALTER TABLE personagens
    ADD COLUMN inspiracao BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN sucessos_contra_morte INT NOT NULL DEFAULT 0,
    ADD COLUMN falhas_contra_morte INT NOT NULL DEFAULT 0;
