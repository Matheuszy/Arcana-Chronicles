package com.arcana.backend.chat.model;

public enum Canal {
    CONVERSA,
    ROLAGENS,
    IMAGENS;

    public static Canal doTipo(ChatMessage.Type tipo) {
        return switch (tipo) {
            case ROLAGEM -> ROLAGENS;
            case IMAGEM -> IMAGENS;
            default -> CONVERSA;
        };
    }
}
