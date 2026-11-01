ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_email_key;

UPDATE usuarios SET email = LOWER(TRIM(email));

CREATE UNIQUE INDEX uk_usuarios_email_papel ON usuarios (email, papel);

CREATE UNIQUE INDEX uk_usuarios_username_sem_caixa ON usuarios (LOWER(username));
