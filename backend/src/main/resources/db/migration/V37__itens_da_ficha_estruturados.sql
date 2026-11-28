CREATE FUNCTION pg_temp.ligar_itens_ao_catalogo(tabela TEXT) RETURNS VOID AS $$
BEGIN
    EXECUTE format(
        'UPDATE %I AS i
            SET catalogo_chave = c.chave,
                item = c.nome,
                peso_kg = round(c.peso_kg / c.quantidade, 4)
           FROM catalogo_equipamentos c
          WHERE i.catalogo_chave IS NULL
            AND c.nome_busca = regexp_replace(
                    translate(lower(btrim(i.item)), ''áàâãäéèêëíìîïóòôõöúùûüçñ'', ''aaaaaeeeeiiiiooooouuuucn''),
                    ''\s+'', '' '', ''g'')',
        tabela);
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.estruturar_itens(tabela TEXT) RETURNS VOID AS $$
BEGIN
    EXECUTE format(
        'ALTER TABLE %I
            ADD COLUMN quantidade INT NOT NULL DEFAULT 1,
            ADD COLUMN peso_kg NUMERIC(8,4),
            ADD COLUMN catalogo_chave VARCHAR(60),
            ADD CONSTRAINT %I FOREIGN KEY (catalogo_chave) REFERENCES catalogo_equipamentos (chave),
            ADD CONSTRAINT %I CHECK (quantidade > 0),
            ADD CONSTRAINT %I CHECK (peso_kg IS NULL OR peso_kg >= 0)',
        tabela, 'fk_' || tabela || '_catalogo', 'ck_' || tabela || '_quantidade', 'ck_' || tabela || '_peso');

    PERFORM pg_temp.ligar_itens_ao_catalogo(tabela);

    EXECUTE format(
        'UPDATE %I
            SET quantidade = substring(item FROM ''\((\d{1,4})\)$'')::INT,
                item = btrim(regexp_replace(item, ''\s*\(\d{1,4}\)$'', ''''))
          WHERE catalogo_chave IS NULL
            AND item ~ ''\S\s*\(0*[1-9]\d{0,3}\)$''',
        tabela);

    EXECUTE format(
        'UPDATE %I
            SET quantidade = substring(item FROM ''\((\d{1,4}) dias?\)$'')::INT,
                item = ''Ração de viagem (1 dia)''
          WHERE catalogo_chave IS NULL
            AND translate(lower(item), ''ãç'', ''ac'') ~ ''^racao de viagem \(0*[1-9]\d{0,3} dias?\)$''',
        tabela);

    PERFORM pg_temp.ligar_itens_ao_catalogo(tabela);
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.estruturar_itens('personagem_itens');
SELECT pg_temp.estruturar_itens('personagem_itens_do_pacote');
SELECT pg_temp.estruturar_itens('pacote_equipamento_itens');

DROP FUNCTION pg_temp.estruturar_itens(TEXT);
DROP FUNCTION pg_temp.ligar_itens_ao_catalogo(TEXT);
