package com.arcana.backend.chat.desafio;

public enum EstadoDesafio {
    AGUARDANDO_ROLAGEM,
    AGUARDANDO_DECISAO_DO_MESTRE,
    AGUARDANDO_TENTATIVA_EXTRA,
    SUCESSO,
    FALHA,
    CANCELADO;

    public boolean ativo() {
        return this == AGUARDANDO_ROLAGEM || this == AGUARDANDO_DECISAO_DO_MESTRE || this == AGUARDANDO_TENTATIVA_EXTRA;
    }

    public boolean aguardandoRolagem() {
        return this == AGUARDANDO_ROLAGEM || this == AGUARDANDO_TENTATIVA_EXTRA;
    }
}
