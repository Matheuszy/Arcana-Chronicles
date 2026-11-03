package com.arcana.backend.friend.repository;

import com.arcana.backend.friend.model.BloqueioUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueioUsuarioRepository extends JpaRepository<BloqueioUsuario, Long> {

    boolean existsByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);

    Optional<BloqueioUsuario> findByBloqueadorIdAndBloqueadoId(Long bloqueadorId, Long bloqueadoId);

    @Query("""
            SELECT b FROM BloqueioUsuario b
            JOIN FETCH b.bloqueado
            WHERE b.bloqueador.id = :bloqueadorId
            ORDER BY b.createdAt DESC
            """)
    List<BloqueioUsuario> findFeitosPor(Long bloqueadorId);

    @Query("""
            SELECT CASE WHEN b.bloqueador.id = :usuarioId THEN b.bloqueado.id ELSE b.bloqueador.id END
            FROM BloqueioUsuario b
            WHERE b.bloqueador.id = :usuarioId OR b.bloqueado.id = :usuarioId
            """)
    List<Long> findIdsBloqueadosEmQualquerSentido(Long usuarioId);
}
