package com.arcana.backend.character.service;

import com.arcana.backend.character.GastoDeDadosDeVidaRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.ResultadoRolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeFichaPublicadaEvent;
import com.arcana.backend.character.RolagemDeVidaRequestDto;
import com.arcana.backend.character.model.ClassePersonagem;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.RolagemDeFicha;
import com.arcana.backend.character.model.TipoRolagemFicha;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.repository.RolagemDeFichaRepository;
import com.arcana.backend.chat.dice.ModoRolagem;
import com.arcana.backend.chat.dice.PedidoRolagem;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.repository.MesaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolagemDeFichaService {

    private static final int RESULTADO_MINIMO_POR_DADO = 1;

    private final PersonagemRepository personagemRepository;
    private final RolagemDeFichaRepository rolagemRepository;
    private final MesaRepository mesaRepository;
    private final RolagemDadosService rolagemDadosService;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public RolagemDeFichaService(PersonagemRepository personagemRepository,
                                 RolagemDeFichaRepository rolagemRepository,
                                 MesaRepository mesaRepository,
                                 RolagemDadosService rolagemDadosService,
                                 ApplicationEventPublisher eventos,
                                 Clock clock) {
        this.personagemRepository = personagemRepository;
        this.rolagemRepository = rolagemRepository;
        this.mesaRepository = mesaRepository;
        this.rolagemDadosService = rolagemDadosService;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public ResultadoRolagemDeFichaDto rolarVida(Long personagemId, Long donoId, RolagemDeVidaRequestDto pedido) {
        Personagem personagem = buscarDoDono(personagemId, donoId);
        int lados = pedido.lados();
        if (!PersonagemService.DADOS_DE_VIDA.contains(lados)) {
            throw new IllegalArgumentException("O dado de vida deve ser d6, d8, d10 ou d12.");
        }
        int modificador = valorOuZero(pedido.modificador());

        List<Integer> dados = rolar(lados, pedido.quantidade());
        int total = somarComMinimoPorDado(dados, modificador);
        personagem.setHpMax(personagem.getHpMax() + total);
        personagem.setHpCurrent(personagem.getHpCurrent() + total);

        return registrar(personagem, donoId, TipoRolagemFicha.VIDA, "Vida máxima +" + total,
                expressao(pedido.quantidade(), lados, modificador), lados, dados, total);
    }

    @Transactional
    public ResultadoRolagemDeFichaDto gastarDadosDeVida(Long personagemId, Long donoId,
                                                        GastoDeDadosDeVidaRequestDto pedido) {
        Personagem personagem = buscarDoDono(personagemId, donoId);
        if (pedido.classe() >= personagem.getClasses().size()) {
            throw new IllegalArgumentException("Essa classe não existe na ficha.");
        }
        ClassePersonagem classe = personagem.getClasses().get(pedido.classe());
        int quantidade = pedido.quantidade();
        if (classe.getDadosVidaDisponiveis() < quantidade) {
            throw new RegraDeNegocioException("Restam " + classe.getDadosVidaDisponiveis()
                    + " dados de vida de " + classe.getClasse() + ".");
        }
        int faltando = personagem.getHpMax() - personagem.getHpCurrent();
        if (faltando <= 0) {
            throw new RegraDeNegocioException("A vida atual já está no máximo.");
        }
        int modificador = valorOuZero(pedido.modificador());

        List<Integer> dados = rolar(classe.getDadoVida(), quantidade);
        int total = somarComMinimoPorDado(dados, modificador);
        int recuperado = Math.min(total, faltando);
        personagem.setHpCurrent(personagem.getHpCurrent() + recuperado);
        classe.setDadosVidaDisponiveis(classe.getDadosVidaDisponiveis() - quantidade);

        String descricao = "Recuperou " + recuperado + " de vida com dados de vida de " + classe.getClasse();
        return registrar(personagem, donoId, TipoRolagemFicha.DADOS_DE_VIDA, descricao,
                expressao(quantidade, classe.getDadoVida(), modificador), classe.getDadoVida(), dados, total);
    }

    @Transactional(readOnly = true)
    public List<RolagemDeFichaDto> listar(Long personagemId, Long donoId) {
        buscarDoDono(personagemId, donoId);
        return historico(personagemId);
    }

    @Transactional(readOnly = true)
    public List<RolagemDeFichaDto> historico(Long personagemId) {
        return rolagemRepository.findTop50ByPersonagemIdOrderByCreatedAtDescIdDesc(personagemId).stream()
                .map(RolagemDeFichaDto::from)
                .toList();
    }

    private ResultadoRolagemDeFichaDto registrar(Personagem personagem, Long donoId, TipoRolagemFicha tipo,
                                                 String descricao, String expressao, int lados,
                                                 List<Integer> dados, int total) {
        String resultados = dados.stream().map(String::valueOf).collect(Collectors.joining(","));
        RolagemDeFicha rolagem = rolagemRepository.save(new RolagemDeFicha(personagem, tipo, descricao, expressao,
                resultados, total, LocalDateTime.now(clock)));
        Personagem salvo = personagemRepository.save(personagem);
        publicarNasMesas(salvo, donoId, expressao, lados, dados, total, descricao);
        return new ResultadoRolagemDeFichaDto(RolagemDeFichaDto.from(rolagem), PersonagemResponseDto.from(salvo));
    }

    private void publicarNasMesas(Personagem personagem, Long donoId, String expressao, int lados,
                                  List<Integer> dados, int total, String descricao) {
        mesaRepository.findByParticipantsCharacterId(personagem.getId()).forEach(mesa ->
                mesa.getParticipants().stream()
                        .filter(p -> donoId.equals(p.getUserId()) && personagem.getId().equals(p.getCharacterId()))
                        .findFirst()
                        .ifPresent(participante -> eventos.publishEvent(new RolagemDeFichaPublicadaEvent(
                                mesa.getId(), donoId, participante.getDisplayName(), expressao, lados, dados,
                                total, personagem.getName() + ": " + descricao))));
    }

    private Personagem buscarDoDono(Long personagemId, Long donoId) {
        Personagem personagem = personagemRepository.findById(personagemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + personagemId));
        if (!personagem.getOwner().getId().equals(donoId)) {
            throw new AccessDeniedException("Personagem não pertence a este usuário.");
        }
        return personagem;
    }

    private List<Integer> rolar(int lados, int quantidade) {
        return rolagemDadosService.rolar(new PedidoRolagem(lados, quantidade, 0, ModoRolagem.NORMAL)).getRolls();
    }

    private int somarComMinimoPorDado(List<Integer> dados, int modificador) {
        return dados.stream().mapToInt(dado -> Math.max(RESULTADO_MINIMO_POR_DADO, dado + modificador)).sum();
    }

    private String expressao(int quantidade, int lados, int modificador) {
        String dado = "1d" + lados + (modificador > 0 ? "+" + modificador : modificador < 0 ? String.valueOf(modificador) : "");
        if (quantidade == 1) return dado;
        return modificador == 0 ? quantidade + "d" + lados : quantidade + "×(" + dado + ")";
    }

    private int valorOuZero(Integer valor) {
        return valor != null ? valor : 0;
    }
}
