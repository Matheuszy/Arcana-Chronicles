package com.arcana.backend.chat.combate;

import java.util.List;

public record CombatesDaMesaDto(
        List<CombateDto> completos,
        List<ResumoCombateDto> resumos
) {}
