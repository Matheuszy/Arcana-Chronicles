package com.arcana.backend;

import com.arcana.backend.character.EquipamentoCatalogoDto;
import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.model.EquipamentoCatalogo;
import com.arcana.backend.character.repository.EquipamentoCatalogoRepository;
import com.arcana.backend.character.service.EquipamentoCatalogoService;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipamentoCatalogoServiceTest {

    private static final EquipamentoCatalogo ESPADA_LONGA = arma("longsword", "Espada longa", "espada longa", 1500);
    private static final EquipamentoCatalogo ESPADA_CURTA = arma("shortsword", "Espada curta", "espada curta", 1000);
    private static final EquipamentoCatalogo MACA_ESTRELA = arma("morningstar", "Maça estrela", "maca estrela", 1500);
    private static final EquipamentoCatalogo MACA = arma("mace", "Maça", "maca", 500);

    @Mock
    private EquipamentoCatalogoRepository repository;

    private EquipamentoCatalogoService service;

    @BeforeEach
    void setUp() {
        service = new EquipamentoCatalogoService(repository);
    }

    private static EquipamentoCatalogo arma(String chave, String nome, String nomeBusca, int precoEmPc) {
        return new EquipamentoCatalogo(chave, nome, nomeBusca, "SRD 5.1", CategoriaEquipamento.ARMA,
                "Arma marcial corpo a corpo", 1, precoEmPc, new BigDecimal("1.500"), "1d8", "Cortante",
                "Versátil (1d10)", null, null, null, null, false);
    }

    private static EquipamentoCatalogo armadura(String subcategoria, int classeArmadura, Boolean somaDestreza,
                                                Integer destrezaMaxima) {
        return new EquipamentoCatalogo("armadura", "Armadura", "armadura", "SRD 5.1", CategoriaEquipamento.ARMADURA,
                subcategoria, 1, 7500, new BigDecimal("27.500"), null, null, null, classeArmadura, somaDestreza,
                destrezaMaxima, 13, true);
    }

    private static EquipamentoCatalogo itemComPreco(int precoEmPc) {
        return new EquipamentoCatalogo("item", "Item", "item", "SRD 5.1", CategoriaEquipamento.EQUIPAMENTO_DE_AVENTURA,
                "Equipamento de aventura", 1, precoEmPc, BigDecimal.ZERO, null, null, null, null, null, null, null,
                false);
    }

    private List<String> nomes(List<EquipamentoCatalogoDto> sugestoes) {
        return sugestoes.stream().map(EquipamentoCatalogoDto::nome).toList();
    }

    @Test
    void deveSugerirPorParteDoNomeComQuemComecaPeloTermoPrimeiroEmOrdemAlfabetica() {
        when(repository.findByNomeBuscaContaining("maca")).thenReturn(List.of(MACA_ESTRELA, MACA));

        assertEquals(List.of("Maça", "Maça estrela"), nomes(service.sugerir("maca", null)));
    }

    @Test
    void devePriorizarNomesQueComecamPeloTermoAntesDosQueSoOContem() {
        EquipamentoCatalogo virotes = arma("crossbow-bolt", "Estojo para virotes", "estojo para virotes", 100);
        when(repository.findByNomeBuscaContaining("e")).thenReturn(List.of(virotes, ESPADA_LONGA, MACA_ESTRELA));

        assertEquals(List.of("Espada longa", "Estojo para virotes", "Maça estrela"), nomes(service.sugerir("e", null)));
    }

    @Test
    void deveIgnorarAcentosMaiusculasEEspacosRepetidosNoTermo() {
        when(repository.findByNomeBuscaContaining("maca estrela")).thenReturn(List.of(MACA_ESTRELA));

        assertEquals(List.of("Maça estrela"), nomes(service.sugerir("  MAÇA   Estrela ", null)));
    }

    @Test
    void deveFiltrarPelaCategoriaQuandoInformada() {
        when(repository.findByNomeBuscaContainingAndCategoria("espada", CategoriaEquipamento.ARMA))
                .thenReturn(List.of(ESPADA_LONGA, ESPADA_CURTA));

        assertEquals(List.of("Espada curta", "Espada longa"),
                nomes(service.sugerir("espada", CategoriaEquipamento.ARMA)));
        verify(repository, never()).findByNomeBuscaContaining(anyString());
    }

    @Test
    void deveLimitarAQuantidadeDeSugestoes() {
        List<EquipamentoCatalogo> muitos = IntStream.range(0, 30)
                .mapToObj(i -> arma("espada-" + i, "Espada " + i, "espada " + i, 100))
                .toList();
        when(repository.findByNomeBuscaContaining("espada")).thenReturn(muitos);

        assertEquals(EquipamentoCatalogoService.LIMITE_DE_SUGESTOES, service.sugerir("espada", null).size());
    }

    @Test
    void naoDeveConsultarOCatalogoComTermoVazioOuLongoDemais() {
        assertTrue(service.sugerir("   ", null).isEmpty());
        assertTrue(service.sugerir(null, CategoriaEquipamento.ARMA).isEmpty());
        assertTrue(service.sugerir("a".repeat(EquipamentoCatalogoService.TAMANHO_MAXIMO_DO_TERMO + 1), null).isEmpty());
        verify(repository, never()).findByNomeBuscaContaining(anyString());
        verify(repository, never()).findByNomeBuscaContainingAndCategoria(anyString(), any());
    }

    @Test
    void deveDetalharPelaChaveSemDiferenciarMaiusculasNemEspacos() {
        when(repository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));

        EquipamentoCatalogoDto detalhe = service.detalhar("  LongSword ");

        assertEquals("Espada longa", detalhe.nome());
        assertEquals("1d8", detalhe.dano());
        assertEquals("Cortante", detalhe.tipoDano());
        assertEquals(new BigDecimal("1.5"), detalhe.pesoKg());
        assertEquals("SRD 5.1", detalhe.versaoRegras());
    }

    @Test
    void deveRecusarChaveQueNaoEstaNoCatalogo() {
        when(repository.findById("inexistente")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.detalhar("inexistente"));
    }

    @Test
    void deveMostrarOPrecoNaMaiorMoedaQueDivideOValorSemSobra() {
        when(repository.findById("item"))
                .thenReturn(Optional.of(itemComPreco(150_000)))
                .thenReturn(Optional.of(itemComPreco(40)))
                .thenReturn(Optional.of(itemComPreco(5)))
                .thenReturn(Optional.of(itemComPreco(0)));

        assertEquals("1.500 po", service.detalhar("item").preco());
        assertEquals("4 pp", service.detalhar("item").preco());
        assertEquals("5 pc", service.detalhar("item").preco());
        assertEquals("0 pc", service.detalhar("item").preco());
    }

    @Test
    void deveDescreverARegraDeClasseDeArmaduraDeCadaTipo() {
        when(repository.findById("armadura"))
                .thenReturn(Optional.of(armadura("Armadura leve", 11, true, null)))
                .thenReturn(Optional.of(armadura("Armadura média", 13, true, 2)))
                .thenReturn(Optional.of(armadura("Armadura pesada", 16, false, null)))
                .thenReturn(Optional.of(armadura("Escudo", 2, false, null)));

        assertEquals("11 + Des", service.detalhar("armadura").regraDeClasseArmadura());
        assertEquals("13 + Des (máx. 2)", service.detalhar("armadura").regraDeClasseArmadura());
        assertEquals("16", service.detalhar("armadura").regraDeClasseArmadura());
        assertEquals("+2", service.detalhar("armadura").regraDeClasseArmadura());
    }

    @Test
    void naoDeveTerRegraDeClasseDeArmaduraParaArmas() {
        when(repository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));

        assertNull(service.detalhar("longsword").regraDeClasseArmadura());
    }
}
