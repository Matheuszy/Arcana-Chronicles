package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonagemRepository extends JpaRepository<Personagem, Long> {

    /** Todos os personagens de um usuário */
    List<Personagem> findByOwnerId(Long ownerId);

    /** Personagens de um usuário filtrados por tipo */
    List<Personagem> findByOwnerIdAndKind(Long ownerId, TipoJogador kind);

    /** Todos os personagens de um tipo (útil para listar NPCs/Monstros da mesa) */
    List<Personagem> findByKind(TipoJogador kind);
}
