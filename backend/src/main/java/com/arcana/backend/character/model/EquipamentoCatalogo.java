package com.arcana.backend.character.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Entity
@Immutable
@Getter
@AllArgsConstructor
@Table(name = "catalogo_equipamentos")
public class EquipamentoCatalogo {

    private static final Pattern DADO_DE_DANO = Pattern.compile("([1-9]|1[0-9]|20)d(4|6|8|10|12|20|100)");
    private static final String ESCUDO = "Escudo";

    @Id
    @Column(length = 60)
    private String chave;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "nome_busca", nullable = false, length = 80)
    private String nomeBusca;

    @Column(name = "versao_regras", nullable = false, length = 20)
    private String versaoRegras;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaEquipamento categoria;

    @Column(nullable = false, length = 40)
    private String subcategoria;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_pc", nullable = false)
    private Integer precoEmPc;

    @Column(name = "peso_kg", nullable = false, precision = 7, scale = 3)
    private BigDecimal pesoKg;

    @Column(length = 10)
    private String dano;

    @Column(name = "tipo_dano", length = 20)
    private String tipoDano;

    @Column(length = 200)
    private String propriedades;

    @Column(name = "classe_armadura")
    private Integer classeArmadura;

    @Column(name = "soma_destreza")
    private Boolean somaDestreza;

    @Column(name = "destreza_maxima")
    private Integer destrezaMaxima;

    @Column(name = "forca_minima")
    private Integer forcaMinima;

    @Column(name = "desvantagem_furtividade", nullable = false)
    private boolean desvantagemFurtividade;

    protected EquipamentoCatalogo() {}

    public TipoEquipamento tipoNaFicha() {
        return switch (categoria) {
            case ARMA -> TipoEquipamento.ARMA;
            case ARMADURA -> TipoEquipamento.ARMADURA;
            default -> null;
        };
    }

    public String danoNaFicha() {
        return dano != null && DADO_DE_DANO.matcher(dano).matches() ? dano : null;
    }

    public String propriedadesNaFicha() {
        List<String> partes = new ArrayList<>();
        if (categoria == CategoriaEquipamento.ARMA) {
            if (dano != null && danoNaFicha() == null) {
                partes.add("dano fixo " + dano);
            }
            if (propriedades != null) {
                partes.add(partes.isEmpty() ? propriedades
                        : Character.toLowerCase(propriedades.charAt(0)) + propriedades.substring(1));
            }
        } else if (categoria == CategoriaEquipamento.ARMADURA) {
            if (Boolean.TRUE.equals(somaDestreza)) {
                partes.add(destrezaMaxima == null ? "+ Des" : "+ Des (máx. " + destrezaMaxima + ")");
            }
            if (forcaMinima != null) {
                partes.add("Força " + forcaMinima);
            }
            if (desvantagemFurtividade) {
                partes.add("desvantagem em Furtividade");
            }
        }
        if (partes.isEmpty()) {
            return null;
        }
        String texto = String.join(", ", partes);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    public String regraDeClasseArmadura() {
        if (classeArmadura == null) {
            return null;
        }
        if (ESCUDO.equals(subcategoria)) {
            return "+" + classeArmadura;
        }
        if (!Boolean.TRUE.equals(somaDestreza)) {
            return String.valueOf(classeArmadura);
        }
        return destrezaMaxima == null
                ? classeArmadura + " + Des"
                : classeArmadura + " + Des (máx. " + destrezaMaxima + ")";
    }
}
