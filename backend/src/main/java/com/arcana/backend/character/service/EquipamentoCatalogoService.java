package com.arcana.backend.character.service;

import com.arcana.backend.character.EquipamentoCatalogoDto;
import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.model.EquipamentoCatalogo;
import com.arcana.backend.character.repository.EquipamentoCatalogoRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class EquipamentoCatalogoService {

    public static final int LIMITE_DE_SUGESTOES = 20;
    public static final int TAMANHO_MAXIMO_DO_TERMO = 60;

    private final EquipamentoCatalogoRepository equipamentoCatalogoRepository;

    public EquipamentoCatalogoService(EquipamentoCatalogoRepository equipamentoCatalogoRepository) {
        this.equipamentoCatalogoRepository = equipamentoCatalogoRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipamentoCatalogoDto> sugerir(String termo, CategoriaEquipamento categoria) {
        String busca = TermoDeBusca.normalizar(termo);
        if (busca.isEmpty() || busca.length() > TAMANHO_MAXIMO_DO_TERMO) {
            return List.of();
        }
        List<EquipamentoCatalogo> encontrados = categoria == null
                ? equipamentoCatalogoRepository.findByNomeBuscaContaining(busca)
                : equipamentoCatalogoRepository.findByNomeBuscaContainingAndCategoria(busca, categoria);
        Comparator<EquipamentoCatalogo> ordem = Comparator
                .comparing((EquipamentoCatalogo equipamento) -> !equipamento.getNomeBusca().startsWith(busca))
                .thenComparing(EquipamentoCatalogo::getNomeBusca);
        return encontrados.stream()
                .sorted(ordem)
                .limit(LIMITE_DE_SUGESTOES)
                .map(EquipamentoCatalogoDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipamentoCatalogoDto detalhar(String chave) {
        return equipamentoCatalogoRepository.findById(chave.trim().toLowerCase(Locale.ROOT))
                .map(EquipamentoCatalogoDto::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Equipamento não encontrado no catálogo: " + chave));
    }
}
