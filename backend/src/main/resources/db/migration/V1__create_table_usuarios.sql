CREATE TABLE IF NOT EXISTS usuarios (
    id        BIGSERIAL    PRIMARY KEY,
    username  VARCHAR(100) NOT NULL UNIQUE,
    email     VARCHAR(250) NOT NULL UNIQUE,
    password  TEXT         NOT NULL
);
