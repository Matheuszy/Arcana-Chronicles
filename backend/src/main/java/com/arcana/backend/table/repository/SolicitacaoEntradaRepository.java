package com.arcana.backend.table.repository;

import com.arcana.backend.table.model.SolicitacaoEntrada;
import com.arcana.backend.table.model.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoEntradaRepository extends JpaRepository<SolicitacaoEntrada, Long> {

    void deleteByUserId(Long userId);

    void deleteByMesaId(Long mesaId);

    boolean existsByMesaIdAndUserIdAndStatus(Long mesaId, Long userId, StatusSolicitacao status);

    Optional<SolicitacaoEntrada> findFirstByMesaIdAndUserIdOrderByCreatedAtDesc(Long mesaId, Long userId);

    List<SolicitacaoEntrada> findByMesaIdAndStatusOrderByCreatedAtAsc(Long mesaId, StatusSolicitacao status);
}
