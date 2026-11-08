package com.arcana.backend.character.repository;

import com.arcana.backend.character.model.PacoteEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PacoteEquipamentoRepository extends JpaRepository<PacoteEquipamento, String> {

    List<PacoteEquipamento> findAllByOrderByPosicaoAsc();
}
