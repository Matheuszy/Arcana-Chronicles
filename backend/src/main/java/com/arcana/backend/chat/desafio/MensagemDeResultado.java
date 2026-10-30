package com.arcana.backend.chat.desafio;

import java.time.Instant;
import java.util.Set;

public record MensagemDeResultado(
        String id,
        String texto,
        Destino destino,
        Set<Long> destinatarios,
        Instant enviadaEm
) {

    public enum Destino {
        JOGADOR,
        VENCEDORES,
        DERROTADOS
    }

    public MensagemDeResultado {
        destinatarios = Set.copyOf(destinatarios);
    }

    public boolean enviadaPara(Long userId) {
        return destinatarios.contains(userId);
    }
}
