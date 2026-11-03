package com.arcana.backend.friend.repository;

import com.arcana.backend.friend.model.SolicitacaoAmizade;
import com.arcana.backend.friend.model.StatusSolicitacaoAmizade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoAmizadeRepository extends JpaRepository<SolicitacaoAmizade, Long> {

    @Query("""
            SELECT s FROM SolicitacaoAmizade s
            WHERE s.status IN :status
              AND ((s.remetente.id = :usuarioId AND s.destinatario.id = :outroId)
                OR (s.remetente.id = :outroId AND s.destinatario.id = :usuarioId))
            """)
    Optional<SolicitacaoAmizade> findEntre(Long usuarioId, Long outroId, Collection<StatusSolicitacaoAmizade> status);

    @Query("""
            SELECT s FROM SolicitacaoAmizade s
            WHERE s.status IN :status
              AND ((s.remetente.id = :usuarioId AND s.destinatario.id IN :outrosIds)
                OR (s.destinatario.id = :usuarioId AND s.remetente.id IN :outrosIds))
            """)
    List<SolicitacaoAmizade> findEntreUsuarioEOutros(
            Long usuarioId, Collection<Long> outrosIds, Collection<StatusSolicitacaoAmizade> status);

    @Query("""
            SELECT CASE WHEN s.remetente.id = :usuarioId THEN s.destinatario.id ELSE s.remetente.id END
            FROM SolicitacaoAmizade s
            WHERE s.status = :status AND (s.remetente.id = :usuarioId OR s.destinatario.id = :usuarioId)
            """)
    List<Long> findIdsDeAmigos(Long usuarioId, StatusSolicitacaoAmizade status);

    @Query("""
            SELECT s FROM SolicitacaoAmizade s
            JOIN FETCH s.remetente
            JOIN FETCH s.destinatario
            WHERE s.status = :status AND (s.remetente.id = :usuarioId OR s.destinatario.id = :usuarioId)
            """)
    List<SolicitacaoAmizade> findAmizadesDe(Long usuarioId, StatusSolicitacaoAmizade status);

    Optional<SolicitacaoAmizade> findFirstByRemetenteIdAndDestinatarioIdAndStatusOrderByDecidedAtDesc(
            Long remetenteId, Long destinatarioId, StatusSolicitacaoAmizade status);

    @Query("""
            SELECT s FROM SolicitacaoAmizade s
            JOIN FETCH s.remetente
            JOIN FETCH s.destinatario
            WHERE s.destinatario.id = :usuarioId AND s.status = :status
            ORDER BY s.createdAt DESC
            """)
    List<SolicitacaoAmizade> findRecebidas(Long usuarioId, StatusSolicitacaoAmizade status);

    @Query("""
            SELECT s FROM SolicitacaoAmizade s
            JOIN FETCH s.remetente
            JOIN FETCH s.destinatario
            WHERE s.remetente.id = :usuarioId AND s.status = :status
            ORDER BY s.createdAt DESC
            """)
    List<SolicitacaoAmizade> findEnviadas(Long usuarioId, StatusSolicitacaoAmizade status);
}
