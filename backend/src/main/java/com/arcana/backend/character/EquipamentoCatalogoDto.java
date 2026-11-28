package com.arcana.backend.character;

import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.model.EquipamentoCatalogo;
import com.arcana.backend.character.model.TipoEquipamento;

import java.math.BigDecimal;
import java.util.Locale;

public record EquipamentoCatalogoDto(
        String chave,
        String nome,
        String versaoRegras,
        CategoriaEquipamento categoria,
        String subcategoria,
        Integer quantidade,
        Integer precoEmPc,
        String preco,
        BigDecimal pesoKg,
        String dano,
        String tipoDano,
        String propriedades,
        Integer classeArmadura,
        Boolean somaDestreza,
        Integer destrezaMaxima,
        String regraDeClasseArmadura,
        Integer forcaMinima,
        boolean desvantagemFurtividade,
        TipoEquipamento tipoNaFicha,
        String danoNaFicha,
        String propriedadesNaFicha
) {

    private static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");
    private static final int PC_POR_PO = 100;
    private static final int PC_POR_PP = 10;

    public static EquipamentoCatalogoDto from(EquipamentoCatalogo equipamento) {
        return new EquipamentoCatalogoDto(equipamento.getChave(), equipamento.getNome(),
                equipamento.getVersaoRegras(), equipamento.getCategoria(), equipamento.getSubcategoria(),
                equipamento.getQuantidade(), equipamento.getPrecoEmPc(), precoFormatado(equipamento.getPrecoEmPc()),
                equipamento.getPesoKg().stripTrailingZeros(), equipamento.getDano(), equipamento.getTipoDano(),
                equipamento.getPropriedades(), equipamento.getClasseArmadura(), equipamento.getSomaDestreza(),
                equipamento.getDestrezaMaxima(), equipamento.regraDeClasseArmadura(), equipamento.getForcaMinima(),
                equipamento.isDesvantagemFurtividade(), equipamento.tipoNaFicha(), equipamento.danoNaFicha(),
                equipamento.propriedadesNaFicha());
    }

    static String precoFormatado(int precoEmPc) {
        if (precoEmPc > 0 && precoEmPc % PC_POR_PO == 0) {
            return String.format(PORTUGUES, "%,d po", precoEmPc / PC_POR_PO);
        }
        if (precoEmPc > 0 && precoEmPc % PC_POR_PP == 0) {
            return String.format(PORTUGUES, "%,d pp", precoEmPc / PC_POR_PP);
        }
        return String.format(PORTUGUES, "%,d pc", precoEmPc);
    }
}
