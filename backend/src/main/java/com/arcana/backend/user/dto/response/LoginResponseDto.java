package com.arcana.backend.user.dto.response;

public record LoginResponseDto(
        Long id,
        String username,
        String email
        // token JWT virá aqui quando Spring Security for adicionado
) {}
