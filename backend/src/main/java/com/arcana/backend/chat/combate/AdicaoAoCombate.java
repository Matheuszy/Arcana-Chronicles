package com.arcana.backend.chat.combate;

import java.util.Set;

public record AdicaoAoCombate(
        EntradaNoCombateDto anuncio,
        Set<Long> jogadoresQueEntraram
) {}
