package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(

        @NotBlank(message = "Username não pode ser vazio")
        String username,

        @NotBlank(message = "Senha não pode ser vazia")
        String password
) {}
