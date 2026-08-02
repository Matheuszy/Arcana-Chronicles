package com.arcana.backend.user.dto.response;

import com.arcana.backend.character.model.Personagem;

import java.util.List;

public record UsuarioResponseDto(
        String username,
        String passwqord,
        List<Personagem> personagens
) {
}
