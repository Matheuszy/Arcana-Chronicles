ALTER TABLE personagens
    ADD COLUMN moedas_roladas BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE personagens p
SET moedas_roladas = TRUE
WHERE EXISTS (
    SELECT 1 FROM ficha_rolagens r
    WHERE r.personagem_id = p.id AND r.tipo = 'MOEDAS'
);
