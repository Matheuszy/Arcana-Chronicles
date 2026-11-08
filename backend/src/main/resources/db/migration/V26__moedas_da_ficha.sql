ALTER TABLE personagens
    ADD COLUMN moedas_cobre   INT NOT NULL DEFAULT 0,
    ADD COLUMN moedas_prata   INT NOT NULL DEFAULT 0,
    ADD COLUMN moedas_electro INT NOT NULL DEFAULT 0,
    ADD COLUMN moedas_ouro    INT NOT NULL DEFAULT 0,
    ADD COLUMN moedas_platina INT NOT NULL DEFAULT 0;
