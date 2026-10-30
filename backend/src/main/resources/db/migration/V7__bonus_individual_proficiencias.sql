ALTER TABLE personagem_pericias ADD COLUMN bonus INT;

UPDATE personagem_pericias pp
SET bonus = p.bonus_proficiencia
FROM personagens p
WHERE p.id = pp.personagem_id;

ALTER TABLE personagem_pericias ALTER COLUMN bonus SET NOT NULL;

ALTER TABLE personagem_salvaguardas ADD COLUMN bonus INT;

UPDATE personagem_salvaguardas ps
SET bonus = p.bonus_proficiencia
FROM personagens p
WHERE p.id = ps.personagem_id;

ALTER TABLE personagem_salvaguardas ALTER COLUMN bonus SET NOT NULL;
