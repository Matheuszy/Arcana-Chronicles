package com.arcana.backend.friend.dto.response;

import com.arcana.backend.presence.model.EstadoPresenca;
import com.arcana.backend.user.model.PapelUsuario;

import java.time.LocalDateTime;

public record AmigoDto(
        Long usuarioId,
        String username,
        PapelUsuario papel,
        EstadoPresenca presenca,
        LocalDateTime desde
) {}
