package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.MagiaCatalogo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface MagiaCatalogoRepository extends Repository<MagiaCatalogo, String> {

    @EntityGraph(attributePaths = "classes")
    List<MagiaCatalogo> findByNomeBuscaContaining(String termo);

    @EntityGraph(attributePaths = "classes")
    List<MagiaCatalogo> findByNomeBuscaContainingAndNivel(String termo, Integer nivel);

    @EntityGraph(attributePaths = "classes")
    List<MagiaCatalogo> findByNomeBuscaContainingAndNivelGreaterThan(String termo, Integer nivel);

    @EntityGraph(attributePaths = "classes")
    Optional<MagiaCatalogo> findById(String chave);
}
