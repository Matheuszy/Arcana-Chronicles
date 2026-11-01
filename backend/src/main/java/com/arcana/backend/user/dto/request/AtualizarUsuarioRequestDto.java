package com.arcana.backend.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequestDto(

        @NotBlank(message = "Username não pode ser vazio")
        @Size(max = 100, message = "Username não pode ter mais de 100 caracteres")
        String username,

        @NotBlank(message = "Email não pode ser vazio")
        @Size(max = 250, message = "Email não pode ter mais de 250 caracteres")
        @Pattern(regexp = FormatoDeConta.EMAIL_VALIDO, message = FormatoDeConta.MENSAGEM_EMAIL_INVALIDO)
        String email,

        @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
        String password
) {

    public AtualizarUsuarioRequestDto {
        username = FormatoDeConta.normalizarUsername(username);
        email = FormatoDeConta.normalizarEmail(email);
    }
}
