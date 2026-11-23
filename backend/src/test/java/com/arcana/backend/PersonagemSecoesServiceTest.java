package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.Equipamento;
import com.arcana.backend.character.model.Magia;
import com.arcana.backend.character.model.MagiaCatalogo;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.MagiaCatalogoRepository;
import com.arcana.backend.character.repository.PacoteEquipamentoRepository;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.secao.SecaoAtributosRequestDto;
import com.arcana.backend.character.secao.SecaoEquipamentoRequestDto;
import com.arcana.backend.character.secao.SecaoHabilidadesRequestDto;
import com.arcana.backend.character.secao.SecaoMagiasRequestDto;
import com.arcana.backend.character.secao.SecaoPersonagemRequestDto;
import com.arcana.backend.character.service.PersonagemService;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonagemSecoesServiceTest {

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    @Mock
    private SessoesAtivasDeMesa sessoesAtivas;

    @Mock
    private PacoteEquipamentoRepository pacoteRepository;

    @Mock
    private MagiaCatalogoRepository magiaCatalogoRepository;

    private PersonagemService service;

    @BeforeEach
    void setUp() {
        service = new PersonagemService(personagemRepository, usuarioRepositorie, new ObjectMapper(), sessoesAtivas,
                pacoteRepository, magiaCatalogoRepository);
    }

    private Personagem fichaCompletaDeAlice() {
        Usuario alice = new Usuario("alice", "alice@arcana.com", "hash");
        alice.setId(1L);

        Personagem personagem = new Personagem();
        personagem.setId(10L);
        personagem.setName("Lyra");
        personagem.setKind(TipoJogador.PLAYER);
        personagem.setLevel(3);
        personagem.setHpMax(20);
        personagem.setHpCurrent(15);
        personagem.setArmorClass(12);
        personagem.setAttributes(new Atributos(10, 14, 12, 10, 16, 8));
        personagem.setOwner(alice);
        personagem.getClasses().add(new ClassePersonagem("Clériga", null, 3, 8, 3));
        personagem.getItens().add("Corda de cânhamo");
        personagem.getEquipamentos().add(new Equipamento(TipoEquipamento.ARMA, "Maça", "1d6", null, null, null));
        personagem.getSpells().add(new Magia("Luz", 0, null));
        personagem.getIdiomas().add("Comum");
        personagem.setMoedasOuro(15);
        return personagem;
    }

    private void prepararFicha(Personagem personagem) {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
    }

    private void salvarDevolvendoAFicha() {
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveAtualizarSoAsMagiasMantendoEquipamentosItensEMoedas() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();

        var resposta = service.atualizarSecaoMagias(10L, new SecaoMagiasRequestDto(
                List.of(new PersonagemRequestDto.SpellDto("Curar Ferimentos", 1, "Cura 1d8")),
                Map.of("1", 3), Map.of("1", 2)), 1L);

        assertEquals("Curar Ferimentos", resposta.spells().get(0).name());
        assertEquals(2, resposta.spellSlotsCurrent().get("1"));
        assertEquals(List.of("Corda de cânhamo"), personagem.getItens());
        assertEquals("Maça", personagem.getEquipamentos().get(0).getNome());
        assertEquals(15, personagem.getMoedasOuro());
        assertEquals(List.of("Comum"), personagem.getIdiomas());
    }

    private static final MagiaCatalogo BOLA_DE_FOGO = new MagiaCatalogo("fireball", "Fireball", "fireball", "SRD 5.1", 3,
            "Evocation", "1 action", "150 feet", "V, S, M", "A tiny ball of bat guano and sulfur.", "Instantaneous",
            false, false, "A bright streak flashes.", "The damage increases by 1d6.",
            new ArrayList<>(List.of("Sorcerer", "Wizard")));

    private PersonagemRequestDto.SpellDto copiaDaBolaDeFogo(String descricao) {
        return new PersonagemRequestDto.SpellDto("Fireball", 3, descricao, "Evocation", "1 action", "150 feet",
                "V, S, M", "A tiny ball of bat guano and sulfur.", "Instantaneous", false, false,
                "The damage increases by 1d6.", "Sorcerer, Wizard", "fireball");
    }

    private SecaoMagiasRequestDto magiasComEspacosDoTerceiroCirculo(List<PersonagemRequestDto.SpellDto> magias) {
        return new SecaoMagiasRequestDto(magias, Map.of("3", 2), Map.of("3", 2));
    }

    @Test
    void deveGuardarCadaCampoDaMagiaCopiadaDoCatalogoSemMarcarComoEditada() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();
        when(magiaCatalogoRepository.findById("fireball")).thenReturn(Optional.of(BOLA_DE_FOGO));

        var magia = service.atualizarSecaoMagias(10L,
                magiasComEspacosDoTerceiroCirculo(List.of(copiaDaBolaDeFogo("A bright streak flashes."))), 1L)
                .spells().get(0);

        assertEquals("150 feet", magia.range());
        assertEquals("1 action", magia.castingTime());
        assertEquals("A tiny ball of bat guano and sulfur.", magia.material());
        assertEquals("Sorcerer, Wizard", magia.classes());
        assertEquals("fireball", magia.catalogKey());
        assertFalse(magia.editada());
    }

    @Test
    void deveMarcarComoEditadaSemAlterarOCatalogo() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();
        when(magiaCatalogoRepository.findById("fireball")).thenReturn(Optional.of(BOLA_DE_FOGO));

        var magia = service.atualizarSecaoMagias(10L,
                magiasComEspacosDoTerceiroCirculo(List.of(copiaDaBolaDeFogo("Minha versão da bola de fogo."))), 1L)
                .spells().get(0);

        assertTrue(magia.editada());
        assertEquals("Minha versão da bola de fogo.", magia.description());
        assertEquals("A bright streak flashes.", BOLA_DE_FOGO.getDescricao());
    }

    @Test
    void deveManterMagiaPersonalizadaSemReferenciaAoCatalogo() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();

        var magia = service.atualizarSecaoMagias(10L, new SecaoMagiasRequestDto(
                List.of(new PersonagemRequestDto.SpellDto("Fireball", 1, "Versão caseira")), Map.of("1", 1), null), 1L)
                .spells().get(0);

        assertNull(magia.catalogKey());
        assertFalse(magia.editada());
    }

    @Test
    void deveRecusarAMesmaMagiaDoCatalogoDuasVezesMasPermitirPersonalizadaComOMesmoNome() {
        prepararFicha(fichaCompletaDeAlice());
        when(magiaCatalogoRepository.findById("fireball")).thenReturn(Optional.of(BOLA_DE_FOGO));

        assertThrows(IllegalArgumentException.class, () -> service.atualizarSecaoMagias(10L,
                magiasComEspacosDoTerceiroCirculo(List.of(copiaDaBolaDeFogo(null), copiaDaBolaDeFogo("outra"))), 1L));
        verify(personagemRepository, never()).save(any());

        salvarDevolvendoAFicha();
        var magias = service.atualizarSecaoMagias(10L, magiasComEspacosDoTerceiroCirculo(List.of(
                copiaDaBolaDeFogo(null), new PersonagemRequestDto.SpellDto("Fireball", 3, "Personalizada"))), 1L)
                .spells();
        assertEquals(2, magias.size());
    }

    @Test
    void deveRecusarMagiaDeCatalogoInexistente() {
        prepararFicha(fichaCompletaDeAlice());
        when(magiaCatalogoRepository.findById("fireball")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.atualizarSecaoMagias(10L,
                magiasComEspacosDoTerceiroCirculo(List.of(copiaDaBolaDeFogo(null))), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveExigirEspacosDeMagiaNaAbaDeMagias() {
        prepararFicha(fichaCompletaDeAlice());

        assertThrows(IllegalArgumentException.class, () -> service.atualizarSecaoMagias(10L,
                new SecaoMagiasRequestDto(List.of(new PersonagemRequestDto.SpellDto("Bênção", 1, null)), null, null),
                1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveAtualizarAbaDePersonagemRecalculandoONivelPelasClasses() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();

        var resposta = service.atualizarSecaoPersonagem(10L, new SecaoPersonagemRequestDto(
                "Lyra Aurora", TipoJogador.PLAYER,
                List.of(new PersonagemRequestDto.ClasseDto("Clériga", "Vida", 4, 8, 4),
                        new PersonagemRequestDto.ClasseDto("Guerreira", null, 1)),
                "Humana", "Leal e Boa", "Acólita", null, null, null, null, "Cresceu no templo.", null, null), 1L);

        assertEquals("Lyra Aurora", resposta.name());
        assertEquals(5, resposta.level());
        assertEquals("Vida", resposta.classes().get(0).subclasse());
        assertEquals("Acólita", personagem.getAntecedente());
        assertEquals(20, personagem.getHpMax());
        assertEquals(1, personagem.getSpells().size());
    }

    @Test
    void deveNegarTrocarParaUmTipoProibidoAoPapelNaAbaDePersonagem() {
        prepararFicha(fichaCompletaDeAlice());

        assertThrows(AccessDeniedException.class, () -> service.atualizarSecaoPersonagem(10L,
                new SecaoPersonagemRequestDto("Lyra", TipoJogador.MONSTER,
                        List.of(new PersonagemRequestDto.ClasseDto("Clériga", null, 3)),
                        "Humana", "Leal e Boa", null, null, null, null, null, null, null, null), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveAtualizarAtributosSemMexerEmHabilidadesNemClasses() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();

        var resposta = service.atualizarSecaoAtributos(10L, new SecaoAtributosRequestDto(
                22, 22, null, 16, 2, 5, 9, null, true, 0, 0, 2,
                new PersonagemRequestDto.AtributosDto(10, 14, 12, 10, 18, 8),
                Map.of("sabedoria", 2), Map.of("percepcao", 2)), 1L);

        assertEquals(18, resposta.classeArmaduraTotal());
        assertEquals(5, resposta.bonusIniciativa());
        assertEquals(5, personagem.getBonusIniciativa());
        assertEquals(16, resposta.percepcaoPassiva());
        assertTrue(resposta.inspiracao());
        assertEquals(List.of("Comum"), personagem.getIdiomas());
        assertEquals(3, personagem.getClasses().get(0).getNivel());
    }

    @Test
    void deveAtualizarEquipamentoMantendoAsMagias() {
        Personagem personagem = fichaCompletaDeAlice();
        prepararFicha(personagem);
        salvarDevolvendoAFicha();

        var resposta = service.atualizarSecaoEquipamento(10L, new SecaoEquipamentoRequestDto(
                List.of(), List.of("  Tocha  "), 1, 2, 0, 30, 0), 1L);

        assertEquals(List.of("Tocha"), resposta.items());
        assertTrue(resposta.equipamentos().isEmpty());
        assertEquals(30, resposta.moedasOuro());
        assertEquals("Luz", personagem.getSpells().get(0).getName());
    }

    @Test
    void deveRecusarIdiomaRepetidoNaAbaDeHabilidades() {
        prepararFicha(fichaCompletaDeAlice());

        assertThrows(IllegalArgumentException.class, () -> service.atualizarSecaoHabilidades(10L,
                new SecaoHabilidadesRequestDto(null, null, null, List.of("Élfico", "élfico"), null, null, null), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveNegarAbasDaFichaDeOutroUsuario() {
        prepararFicha(fichaCompletaDeAlice());

        assertThrows(AccessDeniedException.class, () -> service.buscarSecaoMagias(10L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.atualizarSecaoHabilidades(10L,
                new SecaoHabilidadesRequestDto(null, null, null, null, null, null, null), 2L));
    }

    @Test
    void deveDevolverNaAbaDeRolagensSoOQueAsRolagensUsam() {
        prepararFicha(fichaCompletaDeAlice());

        var resposta = service.buscarSecaoRolagens(10L, 1L);

        assertEquals(15, resposta.hpCurrent());
        assertEquals(12, resposta.attributes().constituicao());
        assertEquals(8, resposta.classes().get(0).dadoVida());
    }
}
