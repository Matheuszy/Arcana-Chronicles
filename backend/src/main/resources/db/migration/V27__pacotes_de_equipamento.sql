CREATE TABLE pacotes_equipamento (
    chave   VARCHAR(30) PRIMARY KEY,
    nome    VARCHAR(60) NOT NULL,
    posicao INT         NOT NULL
);

CREATE TABLE pacote_equipamento_itens (
    pacote_chave VARCHAR(30)  NOT NULL,
    posicao      INT          NOT NULL,
    item         VARCHAR(100) NOT NULL,

    CONSTRAINT fk_itens_pacote_equipamento
        FOREIGN KEY (pacote_chave)
        REFERENCES pacotes_equipamento (chave)
        ON DELETE CASCADE,
    CONSTRAINT pk_pacote_equipamento_itens
        PRIMARY KEY (pacote_chave, posicao)
);

ALTER TABLE personagens
    ADD COLUMN pacote_equipamento VARCHAR(30),
    ADD CONSTRAINT fk_personagens_pacote_equipamento
        FOREIGN KEY (pacote_equipamento)
        REFERENCES pacotes_equipamento (chave);

CREATE TABLE personagem_itens_do_pacote (
    personagem_id BIGINT       NOT NULL,
    posicao       INT          NOT NULL,
    item          VARCHAR(100) NOT NULL,

    CONSTRAINT fk_itens_do_pacote_personagem
        FOREIGN KEY (personagem_id)
        REFERENCES personagens (id)
        ON DELETE CASCADE,
    CONSTRAINT pk_personagem_itens_do_pacote
        PRIMARY KEY (personagem_id, posicao)
);

INSERT INTO pacotes_equipamento (chave, nome, posicao) VALUES
    ('ASSALTANTE', 'Assaltante', 0),
    ('DIPLOMATA', 'Diplomata', 1),
    ('AVENTUREIRO', 'Aventureiro', 2),
    ('ARTISTA', 'Artista', 3),
    ('EXPLORADOR', 'Explorador', 4),
    ('SACERDOTE', 'Sacerdote', 5),
    ('ESTUDIOSO', 'Estudioso', 6);

INSERT INTO pacote_equipamento_itens (pacote_chave, posicao, item) VALUES
    ('ASSALTANTE', 0, 'Mochila'),
    ('ASSALTANTE', 1, 'Rolamentos de esferas (saco com 1.000)'),
    ('ASSALTANTE', 2, 'Barbante (3 m)'),
    ('ASSALTANTE', 3, 'Sino'),
    ('ASSALTANTE', 4, 'Vela (5)'),
    ('ASSALTANTE', 5, 'Pé de cabra'),
    ('ASSALTANTE', 6, 'Martelo'),
    ('ASSALTANTE', 7, 'Pitão (10)'),
    ('ASSALTANTE', 8, 'Lanterna furta-fogo'),
    ('ASSALTANTE', 9, 'Frasco de óleo (2)'),
    ('ASSALTANTE', 10, 'Ração de viagem (5 dias)'),
    ('ASSALTANTE', 11, 'Caixa de isqueiro'),
    ('ASSALTANTE', 12, 'Odre'),
    ('ASSALTANTE', 13, 'Corda de cânhamo (15 m)'),
    ('DIPLOMATA', 0, 'Baú'),
    ('DIPLOMATA', 1, 'Estojo para mapas e pergaminhos (2)'),
    ('DIPLOMATA', 2, 'Roupas finas'),
    ('DIPLOMATA', 3, 'Vidro de tinta'),
    ('DIPLOMATA', 4, 'Pena de escrever'),
    ('DIPLOMATA', 5, 'Lamparina'),
    ('DIPLOMATA', 6, 'Frasco de óleo (2)'),
    ('DIPLOMATA', 7, 'Folha de papel (5)'),
    ('DIPLOMATA', 8, 'Frasco de perfume'),
    ('DIPLOMATA', 9, 'Cera de lacre'),
    ('DIPLOMATA', 10, 'Sabão'),
    ('AVENTUREIRO', 0, 'Mochila'),
    ('AVENTUREIRO', 1, 'Pé de cabra'),
    ('AVENTUREIRO', 2, 'Martelo'),
    ('AVENTUREIRO', 3, 'Pitão (10)'),
    ('AVENTUREIRO', 4, 'Tocha (10)'),
    ('AVENTUREIRO', 5, 'Caixa de isqueiro'),
    ('AVENTUREIRO', 6, 'Ração de viagem (10 dias)'),
    ('AVENTUREIRO', 7, 'Odre'),
    ('AVENTUREIRO', 8, 'Corda de cânhamo (15 m)'),
    ('ARTISTA', 0, 'Mochila'),
    ('ARTISTA', 1, 'Saco de dormir'),
    ('ARTISTA', 2, 'Fantasia (2)'),
    ('ARTISTA', 3, 'Vela (5)'),
    ('ARTISTA', 4, 'Ração de viagem (5 dias)'),
    ('ARTISTA', 5, 'Odre'),
    ('ARTISTA', 6, 'Kit de disfarce'),
    ('EXPLORADOR', 0, 'Mochila'),
    ('EXPLORADOR', 1, 'Saco de dormir'),
    ('EXPLORADOR', 2, 'Kit de refeição'),
    ('EXPLORADOR', 3, 'Caixa de isqueiro'),
    ('EXPLORADOR', 4, 'Tocha (10)'),
    ('EXPLORADOR', 5, 'Ração de viagem (10 dias)'),
    ('EXPLORADOR', 6, 'Odre'),
    ('EXPLORADOR', 7, 'Corda de cânhamo (15 m)'),
    ('SACERDOTE', 0, 'Mochila'),
    ('SACERDOTE', 1, 'Cobertor'),
    ('SACERDOTE', 2, 'Vela (10)'),
    ('SACERDOTE', 3, 'Caixa de isqueiro'),
    ('SACERDOTE', 4, 'Caixa de esmolas'),
    ('SACERDOTE', 5, 'Bloco de incenso (2)'),
    ('SACERDOTE', 6, 'Incensário'),
    ('SACERDOTE', 7, 'Vestes cerimoniais'),
    ('SACERDOTE', 8, 'Ração de viagem (2 dias)'),
    ('SACERDOTE', 9, 'Odre'),
    ('ESTUDIOSO', 0, 'Mochila'),
    ('ESTUDIOSO', 1, 'Livro de conhecimento'),
    ('ESTUDIOSO', 2, 'Vidro de tinta'),
    ('ESTUDIOSO', 3, 'Pena de escrever'),
    ('ESTUDIOSO', 4, 'Folha de pergaminho (10)'),
    ('ESTUDIOSO', 5, 'Saquinho de areia'),
    ('ESTUDIOSO', 6, 'Faca pequena');
