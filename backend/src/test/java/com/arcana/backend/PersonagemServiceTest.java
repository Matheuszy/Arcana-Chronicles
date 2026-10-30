package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.service.PersonagemService;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonagemServiceTest {

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    private PersonagemService service;
    private SessoesAtivasDeMesa sessoesAtivas;

    @BeforeEach
    void setUp() {
        sessoesAtivas = mock(SessoesAtivasDeMesa.class);
        service = new PersonagemService(personagemRepository, usuarioRepositorie, new ObjectMapper(), sessoesAtivas);
    }

    private Personagem personagemDeAlice() {
        Usuario alice = new Usuario("alice", "alice@arcana.com", "hash");
        alice.setId(1L);

        Personagem personagem = new Personagem();
        personagem.setId(10L);
        personagem.setName("Lyra");
        personagem.setKind(TipoJogador.PLAYER);
        personagem.setLevel(1);
        personagem.setHpMax(10);
        personagem.setHpCurrent(10);
        personagem.setArmorClass(12);
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 10, 10));
        personagem.setOwner(alice);
        return personagem;
    }

    private PersonagemRequestDto dtoValido() {
        return dtoDoTipo(TipoJogador.PLAYER);
    }

    private PersonagemRequestDto dtoDoTipo(TipoJogador kind) {
        return new PersonagemRequestDto(
                "Lyra", kind, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }

    private Usuario contaComPapel(Long id, PapelUsuario papel) {
        Usuario usuario = new Usuario("conta" + id, "conta" + id + "@arcana.com", "hash", papel);
        usuario.setId(id);
        when(usuarioRepositorie.findById(id)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    @Test
    void devePermitirJogadorCriarPersonagemDoTipoJogador() {
        contaComPapel(1L, PapelUsuario.JOGADOR);
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.criar(dtoDoTipo(TipoJogador.PLAYER), 1L);

        assertEquals(TipoJogador.PLAYER, dto.kind());
    }

    @Test
    void deveNegarJogadorCriarFichaEnquantoEstaEmUmaMesa() {
        contaComPapel(1L, PapelUsuario.JOGADOR);
        when(sessoesAtivas.usuarioEmSessao(1L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.criar(dtoDoTipo(TipoJogador.PLAYER), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void devePermitirMestreCriarFichaEnquantoEstaEmUmaMesa() {
        contaComPapel(2L, PapelUsuario.MESTRE);
        lenient().when(sessoesAtivas.usuarioEmSessao(2L)).thenReturn(true);
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.criar(dtoDoTipo(TipoJogador.MONSTER), 2L);

        assertEquals(TipoJogador.MONSTER, dto.kind());
    }

    @Test
    void deveNegarJogadorCriarNpc() {
        contaComPapel(1L, PapelUsuario.JOGADOR);

        assertThrows(AccessDeniedException.class, () -> service.criar(dtoDoTipo(TipoJogador.MONSTER), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void devePermitirMestreCriarChefe() {
        contaComPapel(2L, PapelUsuario.MESTRE);
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.criar(dtoDoTipo(TipoJogador.BOSS), 2L);

        assertEquals(TipoJogador.BOSS, dto.kind());
    }

    @Test
    void deveNegarMestreCriarPersonagemDoTipoJogador() {
        contaComPapel(2L, PapelUsuario.MESTRE);

        assertThrows(AccessDeniedException.class, () -> service.criar(dtoDoTipo(TipoJogador.PLAYER), 2L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void devePermitirEditarFichaAntigaMantendoOTipo() {
        Personagem personagem = personagemDeAlice();
        personagem.setKind(TipoJogador.MONSTER);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.atualizar(10L, dtoDoTipo(TipoJogador.MONSTER), 1L);

        assertEquals(TipoJogador.MONSTER, dto.kind());
    }

    @Test
    void deveNegarTrocarTipoParaUmProibidoAoPapel() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.atualizar(10L, dtoDoTipo(TipoJogador.BOSS), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveGravarClasseItensEProficiencias() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PersonagemRequestDto dto = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 5, 30, 30, 15,
                new PersonagemRequestDto.AtributosDto(16, 12, 14, 10, 10, 8),
                Map.of("persuasao", 3, "atletismo", 6), null, null, null, null, null, null, null,
                " Guerreiro ", "Campeão", 3, Map.of("constituicao", 3, "forca", 6),
                List.of(" Corda de 15 m ", "Tocha"), "  Fúria: dobra o dano  ");

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals("Guerreiro", resposta.classe());
        assertEquals("Campeão", resposta.subclasse());
        assertEquals(3, resposta.proficiencyBonus());
        assertEquals(List.of(Map.entry("forca", 6), Map.entry("constituicao", 3)),
                List.copyOf(resposta.savingThrows().entrySet()));
        assertEquals(List.of(Map.entry("atletismo", 6), Map.entry("persuasao", 3)),
                List.copyOf(resposta.skills().entrySet()));
        assertEquals(List.of("Corda de 15 m", "Tocha"), resposta.items());
        assertEquals("Fúria: dobra o dano", resposta.habilidades());
        assertEquals(6, personagem.getSalvaguardas().get(Atributo.FORCA));
    }

    @Test
    void deveRecusarPericiaOuAtributoDesconhecidoNaFicha() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        PersonagemRequestDto comPericiaInvalida = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                Map.of("voar", 2), null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        PersonagemRequestDto comAtributoInvalido = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, Map.of("sorte", 2), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, comPericiaInvalida, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, comAtributoInvalido, 1L));
        verify(personagemRepository, never()).save(any());
    }

    private PersonagemRequestDto dtoComMagias(List<PersonagemRequestDto.SpellDto> magias,
                                              Map<String, Integer> espacos, Map<String, Integer> restantes) {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, magias, espacos, restantes, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void deveExigirEspacosDeMagiaParaCadaCirculoComMagiaAdicionada() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        var bolaDeFogo = List.of(new PersonagemRequestDto.SpellDto("Bola de Fogo", 3, null));

        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(bolaDeFogo, Map.of("1", 4), null), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(bolaDeFogo, Map.of("3", 0), null), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void devePermitirTruquesSemNivelNemEspacosDeMagia() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var truque = List.of(new PersonagemRequestDto.SpellDto("Raio de Fogo", 0, null));

        var resposta = service.atualizar(10L, dtoComMagias(truque, null, null), 1L);

        assertEquals(1, resposta.spells().size());
        assertEquals(0, resposta.spells().get(0).level());
    }

    @Test
    void deveGravarEspacosRestantesEConsiderarTodosDisponiveisQuandoNaoInformados() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var magia = List.of(new PersonagemRequestDto.SpellDto("Mísseis Mágicos", 1, null));

        var gastando = service.atualizar(10L, dtoComMagias(magia, Map.of("1", 4), Map.of("1", 1)), 1L);
        var descansada = service.atualizar(10L, dtoComMagias(magia, Map.of("1", 4), null), 1L);

        assertEquals(1, gastando.spellSlotsCurrent().get("1"));
        assertEquals(4, descansada.spellSlotsCurrent().get("1"));
    }

    @Test
    void deveRecusarEspacosRestantesAcimaDoTotalOuForaDoLimite() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        var magia = List.of(new PersonagemRequestDto.SpellDto("Mísseis Mágicos", 1, null));

        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(magia, Map.of("1", 2), Map.of("1", 3)), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(magia, Map.of("1", 2), Map.of("1", -1)), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(magia, Map.of("1", 21), null), 1L));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(10L, dtoComMagias(magia, Map.of("10", 2), null), 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveUsarBonusDeProficienciaPadraoQuandoNaoInformado() {
        Personagem personagem = personagemDeAlice();
        personagem.setBonusProficiencia(4);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoValido(), 1L);

        assertEquals(2, resposta.proficiencyBonus());
    }

    @Test
    void deveAssociarCadaPericiaAoSeuAtributo() {
        assertEquals(18, Pericia.values().length);
        assertEquals(Atributo.FORCA, Pericia.ATLETISMO.getAtributo());
        assertEquals(Atributo.CARISMA, Pericia.PERSUASAO.getAtributo());
        assertEquals(Pericia.LIDAR_COM_ANIMAIS, Pericia.daChave("lidarComAnimais"));
        assertThrows(IllegalArgumentException.class, () -> Pericia.daChave("voar"));
    }

    @Test
    void devePermitirBuscarPersonagemProprio() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var dto = service.buscarPorId(10L, 1L);

        assertEquals("Lyra", dto.name());
    }

    @Test
    void deveNegarBuscarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.buscarPorId(10L, 2L));
    }

    @Test
    void deveNegarAtualizarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.atualizar(10L, dtoValido(), 2L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveManterAColecaoDePericiasAoAtualizarSemPericias() {
        Personagem personagem = personagemDeAlice();
        personagem.getSkills().put("arcanismo", 2);
        Map<String, Integer> colecaoOriginal = personagem.getSkills();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizar(10L, dtoValido(), 1L);

        assertSame(colecaoOriginal, personagem.getSkills());
        assertTrue(personagem.getSkills().isEmpty());
        assertDoesNotThrow(() -> personagem.getSkills().put("furtividade", 2));
    }

    @Test
    void deveNegarDeletarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.deletar(10L, 2L));
        verify(personagemRepository, never()).delete(any());
    }
}
