ALTER TABLE usuarios ADD COLUMN codigo_amigo VARCHAR(8);

DO $$
DECLARE
    alfabeto CONSTANT TEXT := '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
    usuario_id BIGINT;
    bytes_aleatorios BYTEA;
    codigo TEXT;
    posicao INT;
BEGIN
    FOR usuario_id IN SELECT id FROM usuarios WHERE codigo_amigo IS NULL LOOP
        LOOP
            bytes_aleatorios := uuid_send(gen_random_uuid());
            codigo := '';
            FOR posicao IN 8..15 LOOP
                codigo := codigo || substr(alfabeto, get_byte(bytes_aleatorios, posicao) % 32 + 1, 1);
            END LOOP;
            EXIT WHEN NOT EXISTS (SELECT 1 FROM usuarios WHERE codigo_amigo = codigo);
        END LOOP;
        UPDATE usuarios SET codigo_amigo = codigo WHERE id = usuario_id;
    END LOOP;
END $$;

ALTER TABLE usuarios ALTER COLUMN codigo_amigo SET NOT NULL;

CREATE UNIQUE INDEX uk_usuarios_codigo_amigo ON usuarios (codigo_amigo);
