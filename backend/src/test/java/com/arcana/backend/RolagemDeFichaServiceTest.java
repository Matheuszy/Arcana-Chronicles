package com.arcana.backend;

import com.arcana.backend.character.GastoDeDadosDeVidaRequestDto;
import com.arcana.backend.character.RolagemDeFichaPublicadaEvent;
import com.arcana.backend.character.RolagemDeMoedasRequestDto;
import com.arcana.backend.character.RolagemDeVidaRequestDto;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.Moeda;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.RolagemDeFicha;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.model.TipoRolagemFicha;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.repository.RolagemDeFichaRepository;
import com.arcana.backend.character.service.RolagemDeFichaService;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolagemDeFichaServiceTest {

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private RolagemDeFichaRepository rolagemRepository;

    @Mock
    private MesaRepository mesaRepository;

    private final LinkedList<Integer> faces = new LinkedList<>();
    private final List<Object> eventos = new ArrayList<>();
    private final Set<Long> usuariosEmSessao = new HashSet<>();
    private RolagemDeFichaService service;

    @BeforeEach
    void setUp() {
        RandomGenerator dados = new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                return faces.removeFirst() - 1;
            }
        };
        Clock relogio = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
        service = new RolagemDeFichaService(personagemRepository, rolagemRepository, mesaRepository,
                new RolagemDadosService(dados), eventos::add, relogio, new SessoesAtivasDeMesa() {
                    @Override
                    public boolean possuiSessaoAtiva(Long mesaId) {
                        return false;
                    }

                    @Override
                    public boolean usuarioEmSessao(Long userId) {
                        return usuariosEmSessao.contains(userId);
                    }
                });
    }

    private Personagem fichaDeAlice() {
        Usuario alice = new Usuario("alice", "alice@arcana.com", "hash");
        alice.setId(1L);
        Personagem personagem = new Personagem();
        personagem.setId(10L);
        personagem.setName("Lyra");
        personagem.setKind(TipoJogador.PLAYER);
        personagem.setLevel(3);
        personagem.setHpMax(20);
        personagem.setHpCurrent(12);
        personagem.setArmorClass(12);
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 10, 10));
        personagem.getClasses().add(new ClassePersonagem("Guerreiro", null, 3, 10, 2));
        personagem.setOwner(alice);
        return personagem;
    }

    private void prepararFicha(Personagem personagem) {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        lenient().when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(rolagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveSomarRolagemDeVidaAVidaMaximaEAtualComMinimoDeUmPorDado() {
        Personagem personagem = fichaDeAlice();
        prepararFicha(personagem);
        faces.addAll(List.of(1, 6));

        var resultado = service.rolarVida(10L, 1L, new RolagemDeVidaRequestDto(8, 2, -2));

        assertEquals(5, resultado.rolagem().total());
        assertEquals(List.of(1, 6), resultado.rolagem().resultados());
        assertEquals("2×(1d8-2)", resultado.rolagem().expressao());
        assertEquals(25, resultado.ficha().hpMax());
        assertEquals(17, resultado.ficha().hpCurrent());
        assertEquals(TipoRolagemFicha.VIDA, resultado.rolagem().tipo());
    }

    @Test
    void deveRecusarRolagemDeVidaComDadoQueNaoEDeVida() {
        prepararFicha(fichaDeAlice());

        assertThrows(IllegalArgumentException.class,
                () -> service.rolarVida(10L, 1L, new RolagemDeVidaRequestDto(20, 1, 0)));
        verify(rolagemRepository, never()).save(any());
    }

    @Test
    void deveNegarRolagemDeVidaEmFichaDeOutroUsuario() {
        prepararFicha(fichaDeAlice());

        assertThrows(AccessDeniedException.class,
                () -> service.rolarVida(10L, 2L, new RolagemDeVidaRequestDto(8, 1, 0)));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveGastarDadosDeVidaSemPassarDaVidaMaxima() {
        Personagem personagem = fichaDeAlice();
        prepararFicha(personagem);
        faces.addAll(List.of(7, 9));

        var resultado = service.gastarDadosDeVida(10L, 1L, new GastoDeDadosDeVidaRequestDto(0, 2, 1));

        assertEquals(18, resultado.rolagem().total());
        assertEquals(20, resultado.ficha().hpCurrent());
        assertEquals(20, resultado.ficha().hpMax());
        assertEquals(0, personagem.getClasses().get(0).getDadosVidaDisponiveis());
        assertEquals("Recuperou 8 de vida com dados de vida de Guerreiro", resultado.rolagem().descricao());
    }

    @Test
    void deveRecusarGastarMaisDadosDeVidaQueOsDisponiveis() {
        prepararFicha(fichaDeAlice());

        var erro = assertThrows(RegraDeNegocioException.class,
                () -> service.gastarDadosDeVida(10L, 1L, new GastoDeDadosDeVidaRequestDto(0, 3, 0)));

        assertEquals("Restam 2 dados de vida de Guerreiro.", erro.getMessage());
    }

    @Test
    void deveRecusarGastarDadosDeVidaComVidaCheia() {
        Personagem personagem = fichaDeAlice();
        personagem.setHpCurrent(20);
        prepararFicha(personagem);

        assertThrows(RegraDeNegocioException.class,
                () -> service.gastarDadosDeVida(10L, 1L, new GastoDeDadosDeVidaRequestDto(0, 1, 0)));
        assertEquals(2, personagem.getClasses().get(0).getDadosVidaDisponiveis());
    }

    @Test
    void deveRolarMoedasComMultiplicadorESomarAoValorAtual() {
        Personagem personagem = fichaDeAlice();
        personagem.setMoedasOuro(15);
        prepararFicha(personagem);
        faces.addAll(List.of(1, 2, 3, 4, 4));

        var resultado = service.rolarMoedas(10L, 1L, new RolagemDeMoedasRequestDto(Moeda.OURO, 4, 5, null, 10));

        assertEquals(140, resultado.rolagem().total());
        assertEquals("5d4×10", resultado.rolagem().expressao());
        assertEquals("Ouro +140", resultado.rolagem().descricao());
        assertEquals(155, resultado.ficha().moedasOuro());
        assertTrue(resultado.ficha().moedasRoladas());
        assertEquals(TipoRolagemFicha.MOEDAS, resultado.rolagem().tipo());
    }

    @Test
    void naoDeveTirarMoedasQuandoOModificadorDeixaOResultadoNegativo() {
        Personagem personagem = fichaDeAlice();
        personagem.setMoedasCobre(8);
        prepararFicha(personagem);
        faces.add(2);

        var resultado = service.rolarMoedas(10L, 1L, new RolagemDeMoedasRequestDto(Moeda.COBRE, 6, 1, -5, null));

        assertEquals(0, resultado.rolagem().total());
        assertEquals("1d6-5", resultado.rolagem().expressao());
        assertEquals(8, resultado.ficha().moedasCobre());
    }

    @Test
    void deveRecusarRolagemDeMoedasQuePassaDoLimite() {
        Personagem personagem = fichaDeAlice();
        personagem.setMoedasPlatina(999_990);
        prepararFicha(personagem);
        faces.add(6);

        var erro = assertThrows(IllegalStateException.class,
                () -> service.rolarMoedas(10L, 1L, new RolagemDeMoedasRequestDto(Moeda.PLATINA, 6, 1, 0, 10)));

        assertEquals("As moedas de platina passariam do limite de 1.000.000.", erro.getMessage());
        verify(rolagemRepository, never()).save(any());
    }

    @Test
    void deveRecusarRolarMoedasUmaSegundaVez() {
        Personagem personagem = fichaDeAlice();
        personagem.setMoedasRoladas(true);
        prepararFicha(personagem);

        var erro = assertThrows(RegraDeNegocioException.class,
                () -> service.rolarMoedas(10L, 1L, new RolagemDeMoedasRequestDto(Moeda.OURO, 4, 5, null, 10)));

        assertEquals("As moedas desta ficha já foram roladas. Acrescente moedas conforme a história.", erro.getMessage());
        verify(rolagemRepository, never()).save(any());
    }

    @Test
    void deveRecusarRolarMoedasDuranteAMesa() {
        prepararFicha(fichaDeAlice());
        usuariosEmSessao.add(1L);

        assertThrows(RegraDeNegocioException.class,
                () -> service.rolarMoedas(10L, 1L, new RolagemDeMoedasRequestDto(Moeda.OURO, 4, 5, null, 10)));
        verify(rolagemRepository, never()).save(any());
    }

    @Test
    void deveNegarRolagemDeMoedasEmFichaDeOutroUsuario() {
        prepararFicha(fichaDeAlice());

        assertThrows(AccessDeniedException.class,
                () -> service.rolarMoedas(10L, 2L, new RolagemDeMoedasRequestDto(Moeda.OURO, 6, 1, 0, 1)));
    }

    @Test
    void deveSalvarRolagemNoHistoricoEPublicarNaMesaEmQueAFichaParticipa() {
        Personagem personagem = fichaDeAlice();
        prepararFicha(personagem);
        Mesa mesa = new Mesa();
        mesa.setId(5L);
        mesa.getParticipants().add(new Participante(1L, "alice", "JOGADOR", 10L));
        when(mesaRepository.findByParticipantsCharacterId(10L)).thenReturn(List.of(mesa));
        faces.add(4);

        service.rolarVida(10L, 1L, new RolagemDeVidaRequestDto(10, 1, 3));

        ArgumentCaptor<RolagemDeFicha> salva = ArgumentCaptor.forClass(RolagemDeFicha.class);
        verify(rolagemRepository).save(salva.capture());
        assertEquals("4", salva.getValue().getResultados());
        verify(rolagemRepository).manterApenasAsMaisRecentes(10L, 50);
        assertEquals(7, salva.getValue().getTotal());
        assertEquals(List.of(new RolagemDeFichaPublicadaEvent(5L, 1L, "alice", "1d10+3", 10, List.of(4), 7,
                "Lyra: Vida máxima +7")), eventos);
    }

    @Test
    void naoDevePublicarRolagemQuandoAFichaNaoEstaEmMesa() {
        prepararFicha(fichaDeAlice());
        when(mesaRepository.findByParticipantsCharacterId(10L)).thenReturn(List.of());
        faces.add(3);

        service.rolarVida(10L, 1L, new RolagemDeVidaRequestDto(6, 1, 0));

        assertTrue(eventos.isEmpty());
    }
}
