package com.arcana.backend.chat.sala;

import java.util.List;

public record ComandoSala(Long personagemId, String texto, String nome, String descricao, String itemId,
                          List<Long> destinatarios) {}
