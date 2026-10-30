package com.arcana.backend.table;

public interface SessoesAtivasDeMesa {

    boolean possuiSessaoAtiva(Long mesaId);

    default boolean usuarioEmSessao(Long userId) {
        return false;
    }
}
