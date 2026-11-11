package com.arcana.backend.chat.desafio;

public record Ajuda(
        String id,
        TipoAjuda tipo,
        Long autorId,
        String autorNome,
        Integer faces,
        Integer valor
) {

    public int efeitoNoTotal() {
        return switch (tipo) {
            case DADO_BONUS -> valor;
            case DADO_PENALIDADE -> -valor;
            default -> 0;
        };
    }
}
