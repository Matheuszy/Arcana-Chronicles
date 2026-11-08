package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.RolagemDeFicha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolagemDeFichaRepository extends JpaRepository<RolagemDeFicha, Long> {

    List<RolagemDeFicha> findTop50ByPersonagemIdOrderByCreatedAtDescIdDesc(Long personagemId);

    @Modifying
    @Query(value = """
            DELETE FROM ficha_rolagens
            WHERE personagem_id = :personagemId
              AND id NOT IN (
                  SELECT id FROM ficha_rolagens
                  WHERE personagem_id = :personagemId
                  ORDER BY created_at DESC, id DESC
                  LIMIT :limite
              )
            """, nativeQuery = true)
    int manterApenasAsMaisRecentes(@Param("personagemId") Long personagemId, @Param("limite") int limite);
}
