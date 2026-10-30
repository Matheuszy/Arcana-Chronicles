package com.arcana.backend.chat.desafio;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.table.FichaDeParticipante;

public record EscolhaTeste(Atributo atributo, Pericia pericia) {

    public EscolhaTeste {
        if ((atributo == null) == (pericia == null)) {
            throw new IllegalArgumentException("Escolha um atributo ou uma perícia para o teste.");
        }
    }

    public static EscolhaTeste deAtributo(Atributo atributo) {
        return new EscolhaTeste(atributo, null);
    }

    public static EscolhaTeste dePericia(Pericia pericia) {
        return new EscolhaTeste(null, pericia);
    }

    public int modificadorEm(FichaDeParticipante ficha) {
        return pericia != null ? ficha.modificadorDePericia(pericia) : ficha.modificadorDeAtributo(atributo);
    }

    public String descricao() {
        return pericia != null
                ? pericia.getNome() + " (" + pericia.getAtributo().getNome() + ")"
                : atributo.getNome();
    }
}
