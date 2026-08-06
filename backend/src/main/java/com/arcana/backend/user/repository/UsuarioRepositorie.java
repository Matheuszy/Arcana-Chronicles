package com.arcana.backend.user.repository;

import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.user.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepositorie extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    @Query("SELECT u FROM Usuario u JOIN u.personagens p WHERE p.id = :personagemId")
    Optional<Usuario> findByPersonagemId(Long personagemId);

    @Query("SELECT u FROM Usuario u JOIN u.personagens p WHERE p IN :personagens")
    Optional<Personagem> findByPersonagensIn(List<Personagem> personagens);

}
