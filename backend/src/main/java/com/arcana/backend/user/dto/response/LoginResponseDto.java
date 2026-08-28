package com.arcana.backend.user.dto.response;

public record LoginResponseDto(
        Long id,
        String username,
        String email,
        String token
) {}
