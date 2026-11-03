package com.arcana.backend.table.repository;

import com.arcana.backend.table.model.ConviteMesa;
import com.arcana.backend.table.model.StatusConviteMesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConviteMesaRepository extends JpaRepository<ConviteMesa, Long> {

    boolean existsByMesaIdAndConvidadoIdAndStatus(Long mesaId, Long convidadoId, StatusConviteMesa status);

    @Query("""
            SELECT c FROM ConviteMesa c
            JOIN FETCH c.remetente
            JOIN FETCH c.convidado
            WHERE c.convidado.id = :convidadoId AND c.status = :status
            ORDER BY c.createdAt DESC
            """)
    List<ConviteMesa> findRecebidos(Long convidadoId, StatusConviteMesa status);

    List<ConviteMesa> findByMesaIdAndStatus(Long mesaId, StatusConviteMesa status);

    List<ConviteMesa> findByConvidadoIdAndStatus(Long convidadoId, StatusConviteMesa status);

    @Query("""
            SELECT c FROM ConviteMesa c
            WHERE c.status = :status
              AND ((c.remetente.id = :usuarioId AND c.convidado.id = :outroId)
                OR (c.remetente.id = :outroId AND c.convidado.id = :usuarioId))
            """)
    List<ConviteMesa> findEntre(Long usuarioId, Long outroId, StatusConviteMesa status);
}
