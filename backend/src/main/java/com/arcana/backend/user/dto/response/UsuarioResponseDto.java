package com.arcana.backend.user.dto.response;

import com.arcana.backend.user.model.Usuario;

public record UsuarioResponseDto(
        Long id,
        String username,
        String email
        // senha nunca exposta na resposta
) {
    public static UsuarioResponseDto from(Usuario u) {
        return new UsuarioResponseDto(u.getId(), u.getUsername(), u.getEmail());
    }
}
