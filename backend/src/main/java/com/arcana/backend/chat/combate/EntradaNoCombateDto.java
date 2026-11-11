package com.arcana.backend.chat.combate;

import java.util.List;

public record EntradaNoCombateDto(
        String combateId,
        String combateNome,
        List<String> nomes
) {}
