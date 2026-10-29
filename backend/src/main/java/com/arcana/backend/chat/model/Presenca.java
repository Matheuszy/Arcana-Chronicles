package com.arcana.backend.chat.model;

public record Presenca(
        Long userId,
        String displayName,
        String role
) {}
