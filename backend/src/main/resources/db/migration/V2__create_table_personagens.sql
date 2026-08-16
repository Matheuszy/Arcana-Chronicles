CREATE TABLE IF NOT EXISTS personagens (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(50) NOT NULL, 
    level INT NOT NULL,
    hp_max INT NOT NULL,
    hp_current INT NOT NULL,
    armor_class INT NOT NULL,
    forca INT,
    destreza INT,
    constituicao INT,
    inteligencia INT,
    sabedoria INT,
    carisma INT,
    
    
    equipment TEXT,
    spell_slots TEXT,
    backstory TEXT,
    personality_prompt TEXT,
    
    
    avatar_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS personagem_pericias (
    personagem_id BIGINT NOT NULL,
    pericia VARCHAR(100) NOT NULL,
    CONSTRAINT fk_personagem_pericias 
        FOREIGN KEY (personagem_id) 
        REFERENCES personagens(id) 
        ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS magias (
    id BIGSERIAL PRIMARY KEY,
   
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    
    personagem_id BIGINT NOT NULL,
    CONSTRAINT fk_magia_personagem 
        FOREIGN KEY (personagem_id) 
        REFERENCES personagens(id) 
        ON DELETE CASCADE
);