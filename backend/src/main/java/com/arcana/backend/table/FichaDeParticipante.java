package com.arcana.backend.table;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public record FichaDeParticipante(
        Long jogadorId,
        String nomeJogador,
        Long personagemId,
        String nomePersonagem,
        Map<Atributo, Integer> valores,
        Map<Atributo, Integer> bonusAtributos,
        Map<Pericia, Integer> bonusPericias,
        int bonusProficiencia
) {

    private static final int VALOR_NEUTRO = 10;

    public FichaDeParticipante {
        valores = Map.copyOf(valores);
        bonusAtributos = Map.copyOf(bonusAtributos);
        bonusPericias = Map.copyOf(bonusPericias);
    }

    public static FichaDeParticipante from(Long jogadorId, String nomeJogador, Personagem personagem) {
        Map<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
        for (Atributo atributo : Atributo.values()) {
            Integer valor = personagem.getAttributes() != null ? atributo.valorEm(personagem.getAttributes()) : null;
            valores.put(atributo, valor != null ? valor : VALOR_NEUTRO);
        }

        Map<Pericia, Integer> pericias = new EnumMap<>(Pericia.class);
        Arrays.stream(Pericia.values())
                .filter(pericia -> personagem.getSkills().containsKey(pericia.getChave()))
                .forEach(pericia -> pericias.put(pericia, personagem.getSkills().get(pericia.getChave())));

        return new FichaDeParticipante(jogadorId, nomeJogador, personagem.getId(), personagem.getName(),
                valores, personagem.getSalvaguardas(), pericias, personagem.getBonusProficiencia());
    }

    public static int modificadorDoValor(int valor) {
        return Math.floorDiv(valor - 10, 2);
    }

    public int modificadorBaseDeAtributo(Atributo atributo) {
        return modificadorDoValor(valores.get(atributo));
    }

    public int modificadorDeAtributo(Atributo atributo) {
        return modificadorDoValor(valores.get(atributo)) + bonusAtributos.getOrDefault(atributo, 0);
    }

    public int modificadorDePericia(Pericia pericia) {
        return modificadorDoValor(valores.get(pericia.getAtributo())) + bonusPericias.getOrDefault(pericia, 0);
    }
}
