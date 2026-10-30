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
        ModoRolagem modo,
        String atributo,
        String pericia,
        MensagemDeResultado.Destino destino,
        String texto
) {

    public EscolhaTeste escolha() {
        Atributo atributoEscolhido = vazio(atributo) ? null : Atributo.daChave(atributo);
        Pericia periciaEscolhida = vazio(pericia) ? null : Pericia.daChave(pericia);
        return new EscolhaTeste(atributoEscolhido, periciaEscolhida);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
