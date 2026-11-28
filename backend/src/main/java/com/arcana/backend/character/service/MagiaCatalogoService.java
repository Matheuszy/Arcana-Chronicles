package com.arcana.backend.character.service;

import com.arcana.backend.character.MagiaCatalogoDto;
import com.arcana.backend.character.MagiaCatalogoResumoDto;
import com.arcana.backend.character.model.MagiaCatalogo;
import com.arcana.backend.character.repository.MagiaCatalogoRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Service
public class MagiaCatalogoService {

    public static final int LIMITE_DE_SUGESTOES = 20;
    public static final int TAMANHO_MAXIMO_DO_TERMO = 60;

    private static final Map<String, String> CLASSES_DO_SRD = Map.ofEntries(
            entry("bardo", "Bard"), entry("bard", "Bard"),
            entry("clerigo", "Cleric"), entry("cleric", "Cleric"),
            entry("druida", "Druid"), entry("druid", "Druid"),
            entry("paladino", "Paladin"), entry("paladin", "Paladin"),
            entry("patrulheiro", "Ranger"), entry("guardiao", "Ranger"), entry("ranger", "Ranger"),
            entry("feiticeiro", "Sorcerer"), entry("sorcerer", "Sorcerer"),
            entry("bruxo", "Warlock"), entry("warlock", "Warlock"),
            entry("mago", "Wizard"), entry("wizard", "Wizard"));

    private final MagiaCatalogoRepository magiaCatalogoRepository;

    public MagiaCatalogoService(MagiaCatalogoRepository magiaCatalogoRepository) {
        this.magiaCatalogoRepository = magiaCatalogoRepository;
    }

    @Transactional(readOnly = true)
    public List<MagiaCatalogoResumoDto> sugerir(String termo, Integer nivel, Boolean truques, List<String> classesDaFicha) {
        String busca = normalizar(termo);
        if (busca.isEmpty() || busca.length() > TAMANHO_MAXIMO_DO_TERMO || nivelInvalido(nivel)
                || nivelContradizTruques(nivel, truques)) {
            return List.of();
        }
        Set<String> classes = classesDoSrd(classesDaFicha);
        List<MagiaCatalogo> encontradas = buscar(busca, nivel, truques);
        Comparator<MagiaCatalogo> ordem = Comparator
                .comparing((MagiaCatalogo magia) -> magia.getClasses().stream().noneMatch(classes::contains))
                .thenComparing(magia -> !magia.getNomeBusca().startsWith(busca))
                .thenComparing(MagiaCatalogo::getNome);
        return encontradas.stream()
                .sorted(ordem)
                .limit(LIMITE_DE_SUGESTOES)
                .map(MagiaCatalogoResumoDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MagiaCatalogoDto detalhar(String chave) {
        return magiaCatalogoRepository.findById(chave.trim().toLowerCase(Locale.ROOT))
                .map(MagiaCatalogoDto::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Magia não encontrada no catálogo: " + chave));
    }

    private List<MagiaCatalogo> buscar(String busca, Integer nivel, Boolean truques) {
        if (nivel != null) {
            return magiaCatalogoRepository.findByNomeBuscaContainingAndNivel(busca, nivel);
        }
        if (truques == null) {
            return magiaCatalogoRepository.findByNomeBuscaContaining(busca);
        }
        return truques
                ? magiaCatalogoRepository.findByNomeBuscaContainingAndNivel(busca, 0)
                : magiaCatalogoRepository.findByNomeBuscaContainingAndNivelGreaterThan(busca, 0);
    }

    private static boolean nivelContradizTruques(Integer nivel, Boolean truques) {
        return nivel != null && truques != null && truques != (nivel == 0);
    }

    private static boolean nivelInvalido(Integer nivel) {
        return nivel != null && (nivel < 0 || nivel > 9);
    }

    private static Set<String> classesDoSrd(List<String> classesDaFicha) {
        if (classesDaFicha == null) {
            return Set.of();
        }
        return classesDaFicha.stream()
                .map(MagiaCatalogoService::normalizar)
                .map(CLASSES_DO_SRD::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    static String normalizar(String texto) {
        return TermoDeBusca.normalizar(texto);
    }
}
