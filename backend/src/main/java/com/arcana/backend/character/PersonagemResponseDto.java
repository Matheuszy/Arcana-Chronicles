package com.arcana.backend.character;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoDeslocamento;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PersonagemResponseDto(

        Long id,
        String name,
        TipoJogador kind,
        Integer level,
        Integer hpMax,
        Integer hpCurrent,
        Integer armorClass,
        AtributosDto attributes,
        Map<String, Integer> skills,
        List<EquipamentoDto> equipamentos,
        List<SpellDto> spells,
        Map<String, Integer> spellSlots,
        Map<String, Integer> spellSlotsCurrent,
        String backstory,
        String personalityPrompt,
        String avatarUrl,
        Long ownerId,
        LocalDateTime createdAt,
        List<ClasseDto> classes,
        Integer proficiencyBonus,
        Map<String, Integer> savingThrows,
        List<String> items,
        String habilidades,
        String raca,
        String alinhamento,
        String habilidadesRaca,
        String talentos,
        Integer vidaTemporaria,
        Integer deslocamento,
        boolean inspiracao,
        Integer sucessosContraMorte,
        Integer falhasContraMorte,
        List<String> idiomas,
        List<String> proficienciasFerramentas,
        List<String> proficienciasArmas,
        List<String> proficienciasArmaduras,
        String antecedente,
        String tracosPersonalidade,
        String ideais,
        String vinculos,
        String defeitos,
        int percepcaoPassiva,
        List<DeslocamentoAdicionalDto> deslocamentosAdicionais,
        Integer moedasCobre,
        Integer moedasPrata,
        Integer moedasElectro,
        Integer moedasOuro,
        Integer moedasPlatina,
        String pacoteEquipamento,
        List<String> itensDoPacote,
        Integer caAdicional,
        int classeArmaduraTotal,
        boolean moedasRoladas

) {
    public record ClasseDto(
            String classe,
            String subclasse,
            Integer nivel,
            Integer dadoVida,
            Integer dadosVidaDisponiveis
    ) {}

    public record EquipamentoDto(
            TipoEquipamento tipo,
            String nome,
            String dano,
            String atributo,
            Integer classeArmadura,
            String propriedades
    ) {}

    public record AtributosDto(
            Integer forca,
            Integer destreza,
            Integer constituicao,
            Integer inteligencia,
            Integer sabedoria,
            Integer carisma
    ) {}

    public record DeslocamentoAdicionalDto(
            TipoDeslocamento tipo,
            Integer metros,
            String origem,
            boolean temporario
    ) {}

    public record SpellDto(
            Long id,
            String name,
            Integer level,
            String description
    ) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Converte uma entidade Personagem para o DTO de resposta */
    public static PersonagemResponseDto from(Personagem p) {

        return new PersonagemResponseDto(
                p.getId(),
                p.getName(),
                p.getKind(),
                p.getLevel(),
                p.getHpMax(),
                p.getHpCurrent(),
                p.getArmorClass(),
                atributos(p),
                periciasEmOrdem(p),
                equipamentos(p),
                magias(p),
                espacosDeMagia(p),
                espacosDeMagiaRestantes(p),
                p.getBackstory(),
                p.getPersonalityPrompt(),
                p.getAvatarUrl(),
                p.getOwner() != null ? p.getOwner().getId() : null,
                p.getCreatedAt(),
                classes(p),
                p.getBonusProficiencia(),
                salvaguardasEmOrdem(p),
                List.copyOf(p.getItens()),
                p.getHabilidades(),
                p.getRaca(),
                p.getAlinhamento(),
                p.getHabilidadesRaca(),
                p.getTalentos(),
                p.getVidaTemporaria(),
                p.getDeslocamento(),
                p.isInspiracao(),
                p.getSucessosContraMorte(),
                p.getFalhasContraMorte(),
                List.copyOf(p.getIdiomas()),
                List.copyOf(p.getProficienciasFerramentas()),
                List.copyOf(p.getProficienciasArmas()),
                List.copyOf(p.getProficienciasArmaduras()),
                p.getAntecedente(),
                p.getTracosPersonalidade(),
                p.getIdeais(),
                p.getVinculos(),
                p.getDefeitos(),
                p.calcularPercepcaoPassiva(),
                deslocamentosAdicionais(p),
                p.getMoedasCobre(),
                p.getMoedasPrata(),
                p.getMoedasElectro(),
                p.getMoedasOuro(),
                p.getMoedasPlatina(),
                p.getPacoteEquipamento(),
                List.copyOf(p.getItensDoPacote()),
                p.getCaAdicional(),
                p.getArmorClass() + p.getCaAdicional(),
                p.isMoedasRoladas()
        );
    }

    public static AtributosDto atributos(Personagem p) {
        if (p.getAttributes() == null) return null;
        return new AtributosDto(
                p.getAttributes().getForca(),
                p.getAttributes().getDestreza(),
                p.getAttributes().getConstituicao(),
                p.getAttributes().getInteligencia(),
                p.getAttributes().getSabedoria(),
                p.getAttributes().getCarisma()
        );
    }

    public static List<SpellDto> magias(Personagem p) {
        if (p.getSpells() == null) return Collections.emptyList();
        return p.getSpells().stream()
                .map(m -> new SpellDto(m.getId(), m.getName(), m.getLevel(), m.getDescription()))
                .toList();
    }

    public static Map<String, Integer> espacosDeMagia(Personagem p) {
        return lerEspacos(p.getSpellSlots());
    }

    public static Map<String, Integer> espacosDeMagiaRestantes(Personagem p) {
        return p.getSpellSlotsCurrent() == null ? espacosDeMagia(p) : lerEspacos(p.getSpellSlotsCurrent());
    }

    public static List<EquipamentoDto> equipamentos(Personagem p) {
        return p.getEquipamentos().stream()
                .map(e -> new EquipamentoDto(e.getTipo(), e.getNome(), e.getDano(),
                        e.getAtributo() != null ? e.getAtributo().getChave() : null,
                        e.getClasseArmadura(), e.getPropriedades()))
                .toList();
    }

    public static List<ClasseDto> classes(Personagem p) {
        return p.getClasses().stream()
                .map(c -> new ClasseDto(c.getClasse(), c.getSubclasse(), c.getNivel(),
                        c.getDadoVida(), c.getDadosVidaDisponiveis()))
                .toList();
    }

    public static List<DeslocamentoAdicionalDto> deslocamentosAdicionais(Personagem p) {
        return p.getDeslocamentosAdicionais().stream()
                .map(d -> new DeslocamentoAdicionalDto(d.getTipo(), d.getMetros(), d.getOrigem(), d.isTemporario()))
                .toList();
    }

    private static Map<String, Integer> lerEspacos(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    public static Map<String, Integer> periciasEmOrdem(Personagem p) {
        Map<String, Integer> pericias = new LinkedHashMap<>();
        Arrays.stream(Pericia.values())
                .filter(pericia -> p.getSkills().containsKey(pericia.getChave()))
                .forEach(pericia -> pericias.put(pericia.getChave(), p.getSkills().get(pericia.getChave())));
        return pericias;
    }

    public static Map<String, Integer> salvaguardasEmOrdem(Personagem p) {
        Map<String, Integer> salvaguardas = new LinkedHashMap<>();
        Arrays.stream(Atributo.values())
                .filter(p.getSalvaguardas()::containsKey)
                .forEach(atributo -> salvaguardas.put(atributo.getChave(), p.getSalvaguardas().get(atributo)));
        return salvaguardas;
    }
}
