package com.arcana.backend.user.dto.request;

public record UsuarioRequestDto(
        String username,
        String email,
        String password
) {
}
