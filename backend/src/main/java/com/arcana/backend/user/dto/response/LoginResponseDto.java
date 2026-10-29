package com.arcana.backend.user.dto.response;

import com.arcana.backend.user.model.PapelUsuario;

public record LoginResponseDto(
        Long id,
        String username,
        String email,
        PapelUsuario papel,
        String token
) {}
