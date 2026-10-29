package com.arcana.backend.table;

public record ParticipanteRemovidoEvent(Long mesaId, Long userId, String displayName, boolean removidoPeloMestre) {}
