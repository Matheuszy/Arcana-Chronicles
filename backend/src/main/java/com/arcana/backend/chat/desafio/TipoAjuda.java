package com.arcana.backend.chat.desafio;

public enum TipoAjuda {
    DADO_BONUS(true, true),
    DADO_PENALIDADE(false, true),
    VANTAGEM(true, false),
    DESVANTAGEM(false, false),
    ANULAR_DESVANTAGEM(true, false);

    private final boolean beneficia;
    private final boolean dado;

    TipoAjuda(boolean beneficia, boolean dado) {
        this.beneficia = beneficia;
        this.dado = dado;
    }

    public boolean beneficia() {
        return beneficia;
    }

    public boolean ehDado() {
        return dado;
    }
}
