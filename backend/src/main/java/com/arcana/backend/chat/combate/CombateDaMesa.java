package com.arcana.backend.chat.combate;

import java.util.Optional;
import java.util.function.Consumer;

public class CombateDaMesa {

    private Combate combate;

    public synchronized CombateDto iniciar(Combate novo) {
        if (combate != null) {
            throw new IllegalStateException("Já existe um combate em andamento.");
        }
        combate = novo;
        return CombateDto.de(combate);
    }

    public synchronized CombateDto alterar(Consumer<Combate> alteracao) {
        alteracao.accept(atual());
        return CombateDto.de(combate);
    }

    public synchronized Optional<CombateDto> estado() {
        return Optional.ofNullable(combate).map(CombateDto::de);
    }

    public synchronized boolean podeRolarLivremente(Long userId) {
        return combate == null || combate.podeRolarLivremente(userId);
    }

    public synchronized void removerJogador(Long jogadorId) {
        if (combate == null) return;
        combate.remover(jogadorId);
        if (combate.vazio()) {
            combate = null;
        }
    }

    public synchronized void encerrar() {
        atual();
        combate = null;
    }

    private Combate atual() {
        if (combate == null) {
            throw new IllegalStateException("Não há combate em andamento.");
        }
        return combate;
    }
}
