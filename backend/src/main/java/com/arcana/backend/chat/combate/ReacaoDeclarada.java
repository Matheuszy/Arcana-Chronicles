package com.arcana.backend.chat.combate;

public record ReacaoDeclarada(
        String combateId,
        String nomePersonagem,
        Combate.Reacao reacao
) {}
