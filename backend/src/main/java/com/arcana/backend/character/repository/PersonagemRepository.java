package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.Personagem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonagemRepository extends CrudRepository<Personagem, Long> {

}
