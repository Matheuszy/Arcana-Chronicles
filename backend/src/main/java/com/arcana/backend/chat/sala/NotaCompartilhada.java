package com.arcana.backend.chat.sala;

import java.util.List;

public record NotaCompartilhada(String trecho, List<Long> destinatarios, List<String> nomes) {}
