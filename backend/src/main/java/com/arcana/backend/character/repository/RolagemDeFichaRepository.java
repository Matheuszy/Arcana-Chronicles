package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.RolagemDeFicha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolagemDeFichaRepository extends JpaRepository<RolagemDeFicha, Long> {

    List<RolagemDeFicha> findTop50ByPersonagemIdOrderByCreatedAtDescIdDesc(Long personagemId);
}
