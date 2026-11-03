package com.arcana.backend.friend.dto.response;

import com.arcana.backend.friend.model.RelacaoDeAmizade;
import com.arcana.backend.user.model.PapelUsuario;

public record UsuarioEncontradoDto(
        Long id,
        String username,
        PapelUsuario papel,
        RelacaoDeAmizade relacao,
        Long solicitacaoId
) {}
