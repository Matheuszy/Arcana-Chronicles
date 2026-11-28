package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.model.EquipamentoCatalogo;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface EquipamentoCatalogoRepository extends Repository<EquipamentoCatalogo, String> {

    List<EquipamentoCatalogo> findByNomeBuscaContaining(String termo);

    List<EquipamentoCatalogo> findByNomeBuscaContainingAndCategoria(String termo, CategoriaEquipamento categoria);

    Optional<EquipamentoCatalogo> findById(String chave);
}
