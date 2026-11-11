package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Moeda;
import com.arcana.backend.character.model.PacoteEquipamento;
import com.arcana.backend.character.model.DeslocamentoAdicional;
import com.arcana.backend.character.model.TipoDeslocamento;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.Pericia;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PacoteEquipamentoRepository;
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

    @Mock
    private PacoteEquipamentoRepository pacoteRepository;

    private PersonagemService service;
    private SessoesAtivasDeMesa sessoesAtivas;

    @BeforeEach
    void setUp() {
        sessoesAtivas = mock(SessoesAtivasDeMesa.class);
        service = new PersonagemService(personagemRepository, usuarioRepositorie, new ObjectMapper(), sessoesAtivas,
                pacoteRepository);
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
                null, null, null, null, null, null, null, null, null, null
        , 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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
    void deveManterONivelInformadoQuandoNaoHaClasses() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoValido(), 1L);

        assertEquals(1, resposta.level());
        assertEquals(List.of(), resposta.classes());
    }

    @Test
    void deveSubstituirTodasAsClassesAoAtualizar() {
        Personagem personagem = personagemDeAlice();
        personagem.getClasses().add(new ClassePersonagem("Bárbaro", null, 4));
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PersonagemRequestDto dto = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 4, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", "Evocação", 6)),
                null, null, null, null, null, null, null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals(List.of(new PersonagemResponseDto.ClasseDto("Mago", "Evocação", 6, 8, 6)), resposta.classes());
        assertEquals(6, resposta.level());
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
                "Lyra", TipoJogador.PLAYER, 1, 30, 30, 15,
                new PersonagemRequestDto.AtributosDto(16, 12, 14, 10, 10, 8),
                Map.of("persuasao", 3, "atletismo", 6), null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto(" Guerreiro ", "Campeão", 3),
                        new PersonagemRequestDto.ClasseDto("Artífice", "  ", 2)), 3, Map.of("constituicao", 3, "forca", 6),
                List.of(" Corda de 15 m ", "Tocha"), "  Fúria: dobra o dano  ",
                " Elfo ", "Neutro e Bom", "  Visão no escuro até 18 m  ",
                "  Atleta: +1 em Força  ", 5, 12, true, 2, 1, null, null, null, null,
                " Soldado ", "  Sempre fico de olho nas saídas  ", "Honra", "Minha companhia", "   ", null, null, null, null, null, null, null, null, null);

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals(List.of(new PersonagemResponseDto.ClasseDto("Guerreiro", "Campeão", 3, 8, 3),
                new PersonagemResponseDto.ClasseDto("Artífice", null, 2, 8, 2)), resposta.classes());
        assertEquals(5, resposta.level());
        assertEquals("Atleta: +1 em Força", resposta.talentos());
        assertEquals(3, resposta.proficiencyBonus());
        assertEquals(List.of(Map.entry("forca", 6), Map.entry("constituicao", 3)),
                List.copyOf(resposta.savingThrows().entrySet()));
        assertEquals(List.of(Map.entry("atletismo", 6), Map.entry("persuasao", 3)),
                List.copyOf(resposta.skills().entrySet()));
        assertEquals(List.of("Corda de 15 m", "Tocha"), resposta.items());
        assertEquals("Fúria: dobra o dano", resposta.habilidades());
        assertEquals("Elfo", resposta.raca());
        assertEquals("Neutro e Bom", resposta.alinhamento());
        assertEquals("Visão no escuro até 18 m", resposta.habilidadesRaca());
        assertEquals(5, resposta.vidaTemporaria());
        assertEquals(12, resposta.deslocamento());
        assertTrue(resposta.inspiracao());
        assertEquals(2, resposta.sucessosContraMorte());
        assertEquals(1, resposta.falhasContraMorte());
        assertEquals("Soldado", resposta.antecedente());
        assertEquals("Sempre fico de olho nas saídas", resposta.tracosPersonalidade());
        assertEquals("Honra", resposta.ideais());
        assertEquals("Minha companhia", resposta.vinculos());
        assertNull(resposta.defeitos());
        assertEquals(6, personagem.getSalvaguardas().get(Atributo.FORCA));
    }

    @Test
    void deveLimparInspiracaoETestesContraAMorteQuandoNaoInformados() {
        Personagem personagem = personagemDeAlice();
        personagem.setInspiracao(true);
        personagem.setSucessosContraMorte(3);
        personagem.setFalhasContraMorte(2);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoValido(), 1L);

        assertFalse(resposta.inspiracao());
        assertEquals(0, resposta.sucessosContraMorte());
        assertEquals(0, resposta.falhasContraMorte());
    }

    @Test
    void deveCalcularPercepcaoPassivaComModificadorDeSabedoriaEBonusDeProficiencia() {
        Personagem personagem = personagemDeAlice();
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 15, 10));
        personagem.setBonusProficiencia(3);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertEquals(15, service.buscarPorId(10L, 1L).percepcaoPassiva());
    }

    @Test
    void deveCalcularPercepcaoPassivaComSabedoriaBaixaArredondandoParaBaixo() {
        Personagem personagem = personagemDeAlice();
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 7, 10));
        personagem.setBonusProficiencia(2);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertEquals(10, service.buscarPorId(10L, 1L).percepcaoPassiva());
    }

    @Test
    void deveGravarMoedasEZerarAsQueNaoVieram() {
        Personagem personagem = personagemDeAlice();
        personagem.setMoedasPrata(40);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PersonagemRequestDto dto = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                12, null, 0, 150, 3, null, null, null);

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals(12, resposta.moedasCobre());
        assertEquals(0, resposta.moedasPrata());
        assertEquals(150, resposta.moedasOuro());
        assertEquals(3, resposta.moedasPlatina());
    }

    @Test
    void deveAplicarPacoteEscolhidoNaCriacaoDaFicha() {
        contaComPapel(1L, PapelUsuario.JOGADOR);
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacoteRepository.findById("ARTISTA")).thenReturn(Optional.of(
                new PacoteEquipamento("ARTISTA", "Artista", 3, List.of("Mochila", "Kit de disfarce"))));
        PersonagemRequestDto dto = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, List.of("Alaúde"), null, null, null, null, null, null, 9, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, "artista", null, null);

        var resposta = service.criar(dto, 1L);

        assertEquals(List.of("Alaúde", "Mochila", "Kit de disfarce"), resposta.items());
        assertEquals("ARTISTA", resposta.pacoteEquipamento());
        assertEquals(List.of("Mochila", "Kit de disfarce"), resposta.itensDoPacote());
    }

    @Test
    void deveDefinirCaAdicionalESomarNaCaTotal() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.definirCaAdicional(10L, 1L, 5);

        assertEquals(5, resposta.caAdicional());
        assertEquals(17, resposta.classeArmaduraTotal());
    }

    @Test
    void deveNegarCaAdicionalNaFichaDeOutroUsuario() {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagemDeAlice()));

        assertThrows(AccessDeniedException.class, () -> service.definirCaAdicional(10L, 2L, 2));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveAcrescentarMoedasNarradasNaMesa() {
        Personagem personagem = personagemDeAlice();
        personagem.setMoedasOuro(10);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(35, service.acrescentarMoedas(10L, 1L, Moeda.OURO, 25).moedasOuro());
    }

    @Test
    void deveRecusarAcrescimoQuePassaDoLimiteDeMoedas() {
        Personagem personagem = personagemDeAlice();
        personagem.setMoedasCobre(999_999);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(IllegalStateException.class, () -> service.acrescentarMoedas(10L, 1L, Moeda.COBRE, 2));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveNegarAcrescimoDeMoedasNaFichaDeOutroUsuario() {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagemDeAlice()));

        assertThrows(AccessDeniedException.class, () -> service.acrescentarMoedas(10L, 2L, Moeda.OURO, 5));
    }

    private PersonagemRequestDto dtoComClasse(PersonagemRequestDto.ClasseDto classe) {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(classe), null, null, null, null, null, null, null, null, null, 9, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void deveGravarDadoDeVidaEDadosDisponiveisDaClasse() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L,
                dtoComClasse(new PersonagemRequestDto.ClasseDto("Bárbaro", null, 4, 12, 1)), 1L);

        assertEquals(List.of(new PersonagemResponseDto.ClasseDto("Bárbaro", null, 4, 12, 1)), resposta.classes());
    }

    @Test
    void deveRecusarDadoDeVidaForaDeD6AD12() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L,
                dtoComClasse(new PersonagemRequestDto.ClasseDto("Mago", null, 2, 4, 2)), 1L));

        assertEquals("O dado de vida de Mago deve ser d6, d8, d10 ou d12.", erro.getMessage());
    }

    @Test
    void deveRecusarMaisDadosDeVidaDisponiveisQueONivelDaClasse() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L,
                dtoComClasse(new PersonagemRequestDto.ClasseDto("Mago", null, 2, 6, 3)), 1L));

        assertEquals("Os dados de vida disponíveis de Mago não podem passar do nível da classe (2).", erro.getMessage());
    }

    private PersonagemRequestDto dtoComDeslocamentos(List<PersonagemRequestDto.DeslocamentoAdicionalDto> deslocamentos) {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null,
                null, null, null, null, null, null, null, null, null, deslocamentos, null, null, null, null, null, null, null, null);
    }

    @Test
    void deveGravarDeslocamentosAdicionaisMantendoODeslocamentoBase() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoComDeslocamentos(List.of(
                new PersonagemRequestDto.DeslocamentoAdicionalDto(TipoDeslocamento.VOO, 18, " Magia Voo ", true),
                new PersonagemRequestDto.DeslocamentoAdicionalDto(TipoDeslocamento.NATACAO, 9, "  ", null))), 1L);

        assertEquals(9, resposta.deslocamento());
        assertEquals(List.of(
                new PersonagemResponseDto.DeslocamentoAdicionalDto(TipoDeslocamento.VOO, 18, "Magia Voo", true),
                new PersonagemResponseDto.DeslocamentoAdicionalDto(TipoDeslocamento.NATACAO, 9, null, false)),
                resposta.deslocamentosAdicionais());
    }

    @Test
    void deveRemoverDeslocamentosAdicionaisQuandoAListaVemVazia() {
        Personagem personagem = personagemDeAlice();
        personagem.getDeslocamentosAdicionais().add(new DeslocamentoAdicional(TipoDeslocamento.ESCALADA, 6, null, false));
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoComDeslocamentos(List.of()), 1L);

        assertEquals(List.of(), resposta.deslocamentosAdicionais());
    }

    private PersonagemRequestDto dtoComProficiencias(List<String> idiomas, List<String> ferramentas,
                                                     List<String> armas, List<String> armaduras) {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null,
                idiomas, ferramentas, armas, armaduras, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void deveGravarIdiomasEProficienciasSemEspacosNasPontas() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoComProficiencias(List.of(" Comum ", "Élfico"),
                List.of("Ferramentas de ladrão"), List.of("Armas simples", "Espadas longas"),
                List.of("Armaduras leves")), 1L);

        assertEquals(List.of("Comum", "Élfico"), resposta.idiomas());
        assertEquals(List.of("Ferramentas de ladrão"), resposta.proficienciasFerramentas());
        assertEquals(List.of("Armas simples", "Espadas longas"), resposta.proficienciasArmas());
        assertEquals(List.of("Armaduras leves"), resposta.proficienciasArmaduras());
    }

    @Test
    void deveRecusarIdiomaRepetidoMesmoComOutraCaixa() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L,
                dtoComProficiencias(List.of("Élfico", " élfico"), null, null, null), 1L));

        assertEquals("Idioma repetido na ficha: élfico", erro.getMessage());
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveRecusarProficienciaEmArmaRepetida() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var erro = assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L,
                dtoComProficiencias(null, null, List.of("Arcos", "ARCOS"), null), 1L));

        assertEquals("Proficiência em arma repetida na ficha: ARCOS", erro.getMessage());
    }

    private PersonagemRequestDto dtoComEquipamentos(List<PersonagemRequestDto.EquipamentoDto> equipamentos) {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, equipamentos, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void deveGravarArmasEArmadurasDescartandoOsCamposDoOutroTipo() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var dto = dtoComEquipamentos(List.of(
                new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, " Espada longa ", "1d8", "forca", 15, " Versátil "),
                new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMADURA, "Cota de malha", "2d6", "destreza", 16, "  ")));

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals(List.of(
                new PersonagemResponseDto.EquipamentoDto(TipoEquipamento.ARMA, "Espada longa", "1d8", "forca", null, "Versátil"),
                new PersonagemResponseDto.EquipamentoDto(TipoEquipamento.ARMADURA, "Cota de malha", null, null, 16, null)),
                resposta.equipamentos());
        assertEquals(Atributo.FORCA, personagem.getEquipamentos().get(0).getAtributo());
    }

    @Test
    void devePermitirArmaSemDadoNemAtributo() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var dto = dtoComEquipamentos(List.of(
                new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Adaga", " ", "", null, null)));

        var resposta = service.atualizar(10L, dto, 1L);

        assertEquals(List.of(new PersonagemResponseDto.EquipamentoDto(TipoEquipamento.ARMA, "Adaga", null, null, null, null)),
                resposta.equipamentos());
    }

    @Test
    void deveRecusarArmaComAtributoDesconhecido() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        var dto = dtoComEquipamentos(List.of(
                new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Arco", "1d6", "sorte", null, null)));

        assertThrows(IllegalArgumentException.class, () -> service.atualizar(10L, dto, 1L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveDeixarVidaTemporariaVaziaQuandoNaoInformada() {
        Personagem personagem = personagemDeAlice();
        personagem.setVidaTemporaria(8);
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(10L, dtoValido(), 1L);

        assertNull(resposta.vidaTemporaria());
    }

    @Test
    void deveRecusarPericiaOuAtributoDesconhecidoNaFicha() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        PersonagemRequestDto comPericiaInvalida = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                Map.of("voar", 2), null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        PersonagemRequestDto comAtributoInvalido = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                null, null, Map.of("sorte", 2), null, null, null, null, null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

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
                null, null, null, null, null, null, null, null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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
