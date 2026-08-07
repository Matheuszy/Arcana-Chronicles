package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record UsuarioRequestDto(
        @NotEmpty(message = "O nome de usuário não pode estar vazio")
        String username,
        @NotEmpty(message = "O email não pode estar vazio")
        String email,
        @NotEmpty(message = "A senha não pode estar vazia")
        String password
) {
}