package com.arcana.backend.friend.dto.response;

import com.arcana.backend.friend.model.BloqueioUsuario;

import java.time.LocalDateTime;

public record UsuarioBloqueadoDto(
        Long usuarioId,
        String username,
        LocalDateTime desde
) {
    public static UsuarioBloqueadoDto from(BloqueioUsuario bloqueio) {
        return new UsuarioBloqueadoDto(
                bloqueio.getBloqueado().getId(), bloqueio.getBloqueado().getUsername(), bloqueio.getCreatedAt());
    }
}
