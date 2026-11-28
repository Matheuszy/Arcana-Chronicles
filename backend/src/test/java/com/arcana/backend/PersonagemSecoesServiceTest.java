package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.model.EquipamentoCatalogo;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.Equipamento;
import com.arcana.backend.character.model.ItemDaFicha;
import com.arcana.backend.character.model.Magia;
import com.arcana.backend.character.model.MagiaCatalogo;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.EquipamentoCatalogoRepository;
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

import java.math.BigDecimal;
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

    @Mock
    private EquipamentoCatalogoRepository equipamentoCatalogoRepository;

    private PersonagemService service;

    @BeforeEach
    void setUp() {
        service = new PersonagemService(personagemRepository, usuarioRepositorie, new ObjectMapper(), sessoesAtivas,
                pacoteRepository, magiaCatalogoRepository, equipamentoCatalogoRepository);
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
        personagem.getItens().add(new ItemDaFicha("Corda de cânhamo"));
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
        assertEquals(List.of("Corda de cânhamo"), personagem.getItens().stream().map(ItemDaFicha::getNome).toList());
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
                List.of(), List.of(new PersonagemRequestDto.ItemDto("  Tocha  ")), 1, 2, 0, 30, 0), 1L);

        assertEquals(List.of(new PersonagemResponseDto.ItemDto("Tocha", 1, null, null, false)), resposta.items());
        assertTrue(resposta.equipamentos().isEmpty());
        assertEquals(30, resposta.moedasOuro());
        assertEquals("Luz", personagem.getSpells().get(0).getName());
    }

    private static final EquipamentoCatalogo ESPADA_LONGA = new EquipamentoCatalogo("longsword", "Espada longa",
            "espada longa", "SRD 5.1", CategoriaEquipamento.ARMA, "Arma marcial corpo a corpo", 1, 1500,
            new BigDecimal("1.500"), "1d8", "Cortante", "Versátil (1d10)", null, null, null, null, false);

    private static final EquipamentoCatalogo BRUNEA = new EquipamentoCatalogo("scale-mail", "Brunea", "brunea",
            "SRD 5.1", CategoriaEquipamento.ARMADURA, "Armadura média", 1, 5000, new BigDecimal("22.500"),
            null, null, null, 14, true, 2, null, true);

    private static final EquipamentoCatalogo MOCHILA = new EquipamentoCatalogo("backpack", "Mochila", "mochila",
            "SRD 5.1", CategoriaEquipamento.EQUIPAMENTO_DE_AVENTURA, "Equipamento de aventura", 1, 200,
            new BigDecimal("2.500"), null, null, null, null, null, null, null, false);

    private PersonagemRequestDto.EquipamentoDto espadaLongaDoCatalogo(String pesoKg) {
        return new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Espada longa", "1d8", "forca", null,
                "Versátil (1d10)", "Cortante", new BigDecimal(pesoKg), "longsword");
    }

    private PersonagemRequestDto.EquipamentoDto bruneaDoCatalogo(String pesoKg) {
        return new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMADURA, "Brunea", null, null, 14,
                "+ Des (máx. 2), desvantagem em Furtividade", null, new BigDecimal(pesoKg), "scale-mail");
    }

    private SecaoEquipamentoRequestDto abaDeEquipamento(PersonagemRequestDto.EquipamentoDto... equipamentos) {
        return new SecaoEquipamentoRequestDto(List.of(equipamentos), List.of(), 0, 0, 0, 0, 0);
    }

    @Test
    void deveGuardarArmaCopiadaDoCatalogoComPesoETipoDeDanoSemMarcarComoEditada() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));

        var arma = service.atualizarSecaoEquipamento(10L, abaDeEquipamento(espadaLongaDoCatalogo("1.5")), 1L)
                .equipamentos().get(0);

        assertEquals("longsword", arma.catalogKey());
        assertEquals("Cortante", arma.tipoDano());
        assertEquals(0, new BigDecimal("1.5").compareTo(arma.pesoKg()));
        assertEquals("forca", arma.atributo());
        assertFalse(arma.editada());
    }

    @Test
    void deveMarcarArmaComoEditadaQuandoUmCampoDoCatalogoMuda() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));
        var renomeada = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Espada do avô", "1d8", null,
                null, "Versátil (1d10)", "Cortante", new BigDecimal("1.5"), "longsword");

        var equipamentos = service.atualizarSecaoEquipamento(10L,
                abaDeEquipamento(espadaLongaDoCatalogo("3"), renomeada), 1L).equipamentos();

        assertTrue(equipamentos.get(0).editada());
        assertTrue(equipamentos.get(1).editada());
        assertEquals("Espada longa", ESPADA_LONGA.getNome());
    }

    @Test
    void deveCompararArmaduraDoCatalogoPelaCaPropriedadesEPeso() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("scale-mail")).thenReturn(Optional.of(BRUNEA));

        var equipamentos = service.atualizarSecaoEquipamento(10L,
                abaDeEquipamento(bruneaDoCatalogo("22.5"), bruneaDoCatalogo("20")), 1L).equipamentos();

        assertFalse(equipamentos.get(0).editada());
        assertEquals(14, equipamentos.get(0).classeArmadura());
        assertTrue(equipamentos.get(1).editada());
    }

    @Test
    void devePermitirOMesmoEquipamentoDoCatalogoMaisDeUmaVez() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));

        var equipamentos = service.atualizarSecaoEquipamento(10L,
                abaDeEquipamento(espadaLongaDoCatalogo("1.5"), espadaLongaDoCatalogo("1.5")), 1L).equipamentos();

        assertEquals(2, equipamentos.size());
    }

    @Test
    void deveGuardarPesoDeArmaPersonalizadaSemReferenciaAoCatalogo() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        var personalizada = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Foice do ceifador", "2d4",
                "forca", null, null, "Cortante", new BigDecimal("4"), null);

        var arma = service.atualizarSecaoEquipamento(10L, abaDeEquipamento(personalizada), 1L).equipamentos().get(0);

        assertNull(arma.catalogKey());
        assertFalse(arma.editada());
        assertEquals(0, new BigDecimal("4").compareTo(arma.pesoKg()));
    }

    @Test
    void deveIgnorarTipoDeDanoEmArmadura() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        var armadura = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMADURA, "Couraça", null, null, 15,
                null, "Cortante", null, null);

        var salva = service.atualizarSecaoEquipamento(10L, abaDeEquipamento(armadura), 1L).equipamentos().get(0);

        assertNull(salva.tipoDano());
        assertNull(salva.pesoKg());
    }

    @Test
    void deveRecusarItemDoCatalogoComTipoDiferenteDoEquipamento() {
        prepararFicha(fichaCompletaDeAlice());
        when(equipamentoCatalogoRepository.findById("backpack")).thenReturn(Optional.of(MOCHILA));
        when(equipamentoCatalogoRepository.findById("longsword")).thenReturn(Optional.of(ESPADA_LONGA));
        var mochilaComoArma = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Mochila", null, null,
                null, null, null, new BigDecimal("2.5"), "backpack");
        var espadaComoArmadura = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMADURA, "Espada longa",
                null, null, 2, null, null, new BigDecimal("1.5"), "longsword");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.atualizarSecaoEquipamento(10L, abaDeEquipamento(mochilaComoArma), 1L));
        assertEquals("Mochila do catálogo não é uma arma.", erro.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizarSecaoEquipamento(10L, abaDeEquipamento(espadaComoArmadura), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveRecusarEquipamentoDeCatalogoInexistente() {
        prepararFicha(fichaCompletaDeAlice());
        when(equipamentoCatalogoRepository.findById("longsword")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.atualizarSecaoEquipamento(10L,
                abaDeEquipamento(espadaLongaDoCatalogo("1.5")), 1L));
        verify(personagemRepository, never()).save(any());
    }

    private static final EquipamentoCatalogo FLECHAS = new EquipamentoCatalogo("arrow", "Flechas", "flechas",
            "SRD 5.1", CategoriaEquipamento.EQUIPAMENTO_DE_AVENTURA, "Munição", 20, 100,
            new BigDecimal("0.500"), null, null, null, null, null, null, null, false);

    private SecaoEquipamentoRequestDto abaDeItens(PersonagemRequestDto.ItemDto... itens) {
        return new SecaoEquipamentoRequestDto(List.of(), List.of(itens), 0, 0, 0, 0, 0);
    }

    @Test
    void deveGuardarItemDoCatalogoComQuantidadeEPesoPorUnidadeSemMarcarComoEditado() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("arrow")).thenReturn(Optional.of(FLECHAS));
        when(equipamentoCatalogoRepository.findById("backpack")).thenReturn(Optional.of(MOCHILA));

        var itens = service.atualizarSecaoEquipamento(10L, abaDeItens(
                new PersonagemRequestDto.ItemDto("Flechas", 40, new BigDecimal("0.025"), "arrow"),
                new PersonagemRequestDto.ItemDto("Mochila", null, new BigDecimal("2.5"), "backpack")), 1L).items();

        assertEquals(new PersonagemResponseDto.ItemDto("Flechas", 40, new BigDecimal("0.025"), "arrow", false), itens.get(0));
        assertEquals(1, itens.get(1).quantidade());
        assertFalse(itens.get(1).editado());
    }

    @Test
    void deveMarcarItemDoCatalogoComoEditadoQuandoNomeOuPesoMudam() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();
        when(equipamentoCatalogoRepository.findById("backpack")).thenReturn(Optional.of(MOCHILA));

        var itens = service.atualizarSecaoEquipamento(10L, abaDeItens(
                new PersonagemRequestDto.ItemDto("Mochila de couro", 1, new BigDecimal("2.5"), "backpack"),
                new PersonagemRequestDto.ItemDto("Mochila", 1, new BigDecimal("3"), "backpack"),
                new PersonagemRequestDto.ItemDto("Mochila", 1, null, "backpack")), 1L).items();

        assertTrue(itens.stream().allMatch(PersonagemResponseDto.ItemDto::editado));
    }

    @Test
    void deveGuardarItemPersonalizadoComPesoSemReferenciaAoCatalogo() {
        prepararFicha(fichaCompletaDeAlice());
        salvarDevolvendoAFicha();

        var item = service.atualizarSecaoEquipamento(10L, abaDeItens(
                new PersonagemRequestDto.ItemDto(" Amuleto da avó ", 2, new BigDecimal("0.1"), " ")), 1L).items().get(0);

        assertEquals(new PersonagemResponseDto.ItemDto("Amuleto da avó", 2, new BigDecimal("0.1"), null, false), item);
        verify(equipamentoCatalogoRepository, never()).findById(any());
    }

    @Test
    void deveRecusarItemDeCatalogoInexistente() {
        prepararFicha(fichaCompletaDeAlice());
        when(equipamentoCatalogoRepository.findById("arrow")).thenReturn(Optional.empty());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.atualizarSecaoEquipamento(10L, abaDeItens(
                        new PersonagemRequestDto.ItemDto("Flechas", 20, null, "arrow")), 1L));
        assertEquals("Item do catálogo não encontrado: arrow", erro.getMessage());
        verify(personagemRepository, never()).save(any());
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
