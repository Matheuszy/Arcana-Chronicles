package com.arcana.backend.table.repository;

import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.StatusMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    /** Todas as mesas de um mestre */
    List<Mesa> findByMasterId(Long masterId);

    /** Mesas por status (ex: só as ABERTAS) */
    List<Mesa> findByStatus(StatusMesa status);
}
