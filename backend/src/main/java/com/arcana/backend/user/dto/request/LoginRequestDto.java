package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(

        @NotBlank(message = "Username não pode ser vazio")
        String username,

        @NotBlank(message = "Senha não pode ser vazia")
        @Size(max = 72, message = "Usuário ou senha inválidos.")
        String password
) {}
