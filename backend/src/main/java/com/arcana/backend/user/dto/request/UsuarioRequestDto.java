package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.Email;
import com.arcana.backend.user.model.PapelUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDto(

        @NotBlank(message = "Username não pode ser vazio")
        @Size(max = 100)
        String username,

        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha não pode ser vazia")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        String password,

        @NotNull(message = "Papel é obrigatório: MESTRE ou JOGADOR")
        PapelUsuario papel
) {}
