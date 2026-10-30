package com.arcana.backend.chat.desafio;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record ComandoDesafio(
        Integer cd,
        String descricao,
        List<NovoDesafio.Convocacao> jogadores,
        String desafioId,
        Long jogadorId,
        String alvoId,
        List<String> alvos,
        ModoRolagem modo,
        String atributo,
        String pericia,
        MensagemDeResultado.Destino destino,
        String texto
) {

    public String alvo() {
        if (alvoId != null && !alvoId.isBlank()) {
            return alvoId.trim();
        }
        return jogadorId == null ? null : Desafio.idDoJogador(jogadorId);
    }

    public EscolhaTeste escolha() {
        Atributo atributoEscolhido = vazio(atributo) ? null : Atributo.daChave(atributo);
        Pericia periciaEscolhida = vazio(pericia) ? null : Pericia.daChave(pericia);
        return new EscolhaTeste(atributoEscolhido, periciaEscolhida);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
