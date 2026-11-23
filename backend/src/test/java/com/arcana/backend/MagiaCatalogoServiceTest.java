package com.arcana.backend;

import com.arcana.backend.character.MagiaCatalogoResumoDto;
import com.arcana.backend.character.model.MagiaCatalogo;
import com.arcana.backend.character.repository.MagiaCatalogoRepository;
import com.arcana.backend.character.service.MagiaCatalogoService;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MagiaCatalogoServiceTest {

    private static final MagiaCatalogo FIREBALL = magia("fireball", "Fireball", 3, List.of("Sorcerer", "Wizard"));
    private static final MagiaCatalogo FIRE_BOLT = magia("fire-bolt", "Fire Bolt", 0, List.of("Sorcerer", "Wizard"));
    private static final MagiaCatalogo FIRE_STORM = magia("fire-storm", "Fire Storm", 7, List.of("Cleric", "Druid"));
    private static final MagiaCatalogo DELAYED_BLAST = magia("delayed-blast-fireball", "Delayed Blast Fireball", 7,
            List.of("Sorcerer", "Wizard"));

    @Mock
    private MagiaCatalogoRepository repository;

    private MagiaCatalogoService service;

    @BeforeEach
    void setUp() {
        service = new MagiaCatalogoService(repository);
    }

    private static MagiaCatalogo magia(String chave, String nome, int nivel, List<String> classes) {
        return new MagiaCatalogo(chave, nome, nome.toLowerCase(), "SRD 5.1", nivel, "Evocation", "1 action",
                "150 feet", "V, S, M", "A tiny ball of bat guano and sulfur.", "Instantaneous", false, false,
                "A bright streak flashes.", null, new ArrayList<>(classes));
    }

    private List<String> nomes(List<MagiaCatalogoResumoDto> sugestoes) {
        return sugestoes.stream().map(MagiaCatalogoResumoDto::nome).toList();
    }

    @Test
    void deveSugerirPorParteDoNomeComQuemComecaPeloTermoPrimeiro() {
        when(repository.findByNomeBuscaContaining("fire"))
                .thenReturn(List.of(DELAYED_BLAST, FIRE_STORM, FIREBALL, FIRE_BOLT));

        var sugestoes = service.sugerir("  FIRE ", null, null, null);

        assertEquals(List.of("Fire Bolt", "Fire Storm", "Fireball", "Delayed Blast Fireball"), nomes(sugestoes));
        assertEquals(List.of("Sorcerer", "Wizard"), sugestoes.get(0).classes());
    }

    @Test
    void deveIgnorarAcentosEEspacosRepetidosNoTermo() {
        when(repository.findByNomeBuscaContaining("fire bolt")).thenReturn(List.of(FIRE_BOLT));

        assertEquals(List.of("Fire Bolt"), nomes(service.sugerir("Fíre   Bólt", null, null, null)));
    }

    @Test
    void devePriorizarAsClassesDaFichaEscritasEmPortuguesSemBloquearAsDemais() {
        when(repository.findByNomeBuscaContaining("fire"))
                .thenReturn(List.of(DELAYED_BLAST, FIRE_STORM, FIREBALL, FIRE_BOLT));

        var sugestoes = service.sugerir("fire", null, null, List.of(" CLÉRIGO ", "Classe inventada"));

        assertEquals(List.of("Fire Storm", "Fire Bolt", "Fireball", "Delayed Blast Fireball"), nomes(sugestoes));
    }

    @Test
    void deveAceitarAsClassesDaFichaEscritasEmIngles() {
        when(repository.findByNomeBuscaContaining("fire")).thenReturn(List.of(FIREBALL, FIRE_STORM));

        assertEquals(List.of("Fire Storm", "Fireball"), nomes(service.sugerir("fire", null, null, List.of("druid"))));
    }

    @Test
    void deveFiltrarPorNivelQuandoInformado() {
        when(repository.findByNomeBuscaContainingAndNivel("fire", 0)).thenReturn(List.of(FIRE_BOLT));

        assertEquals(List.of("Fire Bolt"), nomes(service.sugerir("fire", 0, null, null)));
        verify(repository, never()).findByNomeBuscaContaining(anyString());
    }

    @Test
    void deveSepararTruquesDasMagiasDeCirculoQuandoPedido() {
        when(repository.findByNomeBuscaContainingAndNivel("fire", 0)).thenReturn(List.of(FIRE_BOLT));
        when(repository.findByNomeBuscaContainingAndNivelGreaterThan("fire", 0))
                .thenReturn(List.of(FIREBALL, FIRE_STORM));

        assertEquals(List.of("Fire Bolt"), nomes(service.sugerir("fire", null, true, null)));
        assertEquals(List.of("Fire Storm", "Fireball"), nomes(service.sugerir("fire", null, false, null)));
        assertTrue(service.sugerir("fire", 3, true, null).isEmpty());
        assertTrue(service.sugerir("fire", 0, false, null).isEmpty());
    }

    @Test
    void deveLimitarAQuantidadeDeSugestoes() {
        List<MagiaCatalogo> muitas = IntStream.range(0, 30)
                .mapToObj(i -> magia("magia-" + i, "Magia " + (100 + i), 1, List.of("Wizard")))
                .toList();
        when(repository.findByNomeBuscaContaining("magia")).thenReturn(muitas);

        var sugestoes = service.sugerir("magia", null, null, null);

        assertEquals(MagiaCatalogoService.LIMITE_DE_SUGESTOES, sugestoes.size());
        assertEquals("Magia 100", sugestoes.get(0).nome());
    }

    @Test
    void deveDevolverListaVaziaSemConsultarOBancoParaTermoVazioLongoOuNivelInvalido() {
        assertTrue(service.sugerir("   ", null, null, null).isEmpty());
        assertTrue(service.sugerir(null, null, null, null).isEmpty());
        assertTrue(service.sugerir("a".repeat(MagiaCatalogoService.TAMANHO_MAXIMO_DO_TERMO + 1), null, null, null).isEmpty());
        assertTrue(service.sugerir("fire", 10, null, null).isEmpty());
        assertTrue(service.sugerir("fire", -1, null, null).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void deveDevolverListaVaziaQuandoNenhumaMagiaCombina() {
        when(repository.findByNomeBuscaContaining("xyz")).thenReturn(List.of());

        assertTrue(service.sugerir("xyz", null, null, List.of("Mago")).isEmpty());
    }

    @Test
    void deveDetalharMagiaPelaChaveComTodosOsCampos() {
        when(repository.findById("fireball")).thenReturn(Optional.of(FIREBALL));

        var detalhe = service.detalhar(" FireBall ");

        assertEquals("Fireball", detalhe.nome());
        assertEquals(3, detalhe.nivel());
        assertEquals("150 feet", detalhe.alcance());
        assertEquals("V, S, M", detalhe.componentes());
        assertEquals("A tiny ball of bat guano and sulfur.", detalhe.material());
        assertEquals("SRD 5.1", detalhe.versaoRegras());
    }

    @Test
    void deveResponderNaoEncontradoParaChaveInexistente() {
        when(repository.findById("wish-2")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.detalhar("wish-2"));
    }
}
