package com.arcana.backend.chat.sessao;

import java.util.Collection;

public record UsuariosEmMesaAlteradosEvent(Collection<Long> usuarioIds) {}
