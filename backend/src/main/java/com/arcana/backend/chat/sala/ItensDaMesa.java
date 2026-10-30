package com.arcana.backend.chat.sala;

import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItensDaMesa {

    public static final int LIMITE_ITENS = 50;
    public static final int LIMITE_NOME = 60;
    public static final int LIMITE_DESCRICAO = 500;

    private final List<ItemDto> itens = new ArrayList<>();

    public synchronized ItemDto adicionar(Long autorId, String autorNome, String nome, String descricao,
                                          Instant agora) {
        String nomeLimpo = nome == null ? "" : nome.trim();
        String descricaoLimpa = descricao == null ? "" : descricao.trim();
        if (nomeLimpo.isEmpty()) {
            throw new IllegalArgumentException("Dê um nome ao item.");
        }
        if (nomeLimpo.length() > LIMITE_NOME) {
            throw new IllegalArgumentException("O nome do item passa do limite de " + LIMITE_NOME + " caracteres.");
        }
        if (descricaoLimpa.length() > LIMITE_DESCRICAO) {
            throw new IllegalArgumentException(
                    "A descrição do item passa do limite de " + LIMITE_DESCRICAO + " caracteres.");
        }
        if (itens.size() >= LIMITE_ITENS) {
            throw new IllegalStateException("A mesa já tem " + LIMITE_ITENS + " itens compartilhados.");
        }

        ItemDto item = new ItemDto(UUID.randomUUID().toString(), nomeLimpo, descricaoLimpa,
                String.valueOf(autorId), autorNome, agora.toString());
        itens.add(item);
        return item;
    }

    public synchronized ItemDto remover(String itemId, Long userId, boolean mestre) {
        ItemDto item = itens.stream()
                .filter(candidato -> candidato.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Este item não existe mais."));
        if (!mestre && !item.autorId().equals(String.valueOf(userId))) {
            throw new AccessDeniedException("Apenas quem criou o item ou o mestre pode removê-lo.");
        }
        itens.remove(item);
        return item;
    }

    public synchronized List<ItemDto> lista() {
        return List.copyOf(itens);
    }
}
