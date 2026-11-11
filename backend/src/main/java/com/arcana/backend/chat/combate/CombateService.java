package com.arcana.backend.chat.combate;

import com.arcana.backend.character.model.Atributo;
import com.arcana.backend.chat.combate.ComandoCombate.GrupoDeMonstros;
import com.arcana.backend.chat.dice.ModoRolagem;
import com.arcana.backend.chat.dice.PedidoRolagem;
import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.model.ChatMessage.DiceRollResult;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.FichaDeParticipante;
import com.arcana.backend.table.service.MesaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CombateService {

    static final int MAXIMO_MONSTROS_POR_GRUPO = 20;
    static final int MAXIMO_CARACTERES_DO_NOME = 60;
    static final int MAXIMO_CARACTERES_DA_REACAO = 80;

    private final MesaService mesaService;
    private final RolagemDadosService rolagemDados;
    private final Clock clock;

    public CombateService(MesaService mesaService, RolagemDadosService rolagemDados, Clock clock) {
        this.mesaService = mesaService;
        this.rolagemDados = rolagemDados;
        this.clock = clock;
    }

    public CombateDto iniciar(MesaAtiva mesa, AcessoMesaDto mestre, String nome, List<Long> jogadorIds,
                              List<GrupoDeMonstros> grupos) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        String nomeEscolhido = validarNomeDoCombate(nome);
        List<Long> escolhidos = validarJogadores(jogadorIds);
        List<GrupoDeMonstros> gruposEscolhidos = validarGrupos(grupos);

        Combate combate = new Combate(UUID.randomUUID().toString(),
                nomeEscolhido != null ? nomeEscolhido : mesa.combate().proximoNomePadrao(),
                mestre.userId(), clock.instant());
        for (Long jogadorId : escolhidos) {
            FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId);
            combate.adicionarJogador(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(),
                    ficha.modificadorBaseDeAtributo(Atributo.DESTREZA), ficha.bonusIniciativa());
        }
        for (GrupoDeMonstros grupo : gruposEscolhidos) {
            adicionarGrupo(combate, grupo, fichaDoGrupo(mesa, mestre, grupo));
        }
        return mesa.combate().iniciar(combate);
    }

    public AdicaoAoCombate adicionar(MesaAtiva mesa, AcessoMesaDto mestre, String combateId, List<Long> jogadorIds,
                                     List<GrupoDeMonstros> grupos) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<Long> escolhidos = jogadorIds == null || jogadorIds.isEmpty() ? List.of() : validarJogadores(jogadorIds);
        List<GrupoDeMonstros> gruposEscolhidos = validarGrupos(grupos);
        if (escolhidos.isEmpty() && gruposEscolhidos.isEmpty()) {
            throw new IllegalArgumentException("Escolha ao menos um jogador ou monstro para entrar no combate.");
        }

        List<FichaDeParticipante> fichasDosJogadores = escolhidos.stream()
                .map(jogadorId -> mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId))
                .toList();
        Map<GrupoDeMonstros, FichaDeParticipante> fichasDosGrupos = new LinkedHashMap<>();
        gruposEscolhidos.forEach(grupo -> fichasDosGrupos.put(grupo, fichaDoGrupo(mesa, mestre, grupo)));
        Map<Long, String> jogadoresComPersonagem = new LinkedHashMap<>();
        fichasDosJogadores.forEach(ficha -> jogadoresComPersonagem.put(ficha.jogadorId(), ficha.nomePersonagem()));

        List<String> nomes = new ArrayList<>();
        Combate combate = mesa.combate().adicionar(combateId, jogadoresComPersonagem, alvo -> {
            Set<String> anteriores = new HashSet<>(alvo.entradasEmOrdem().stream().map(Combate.Entrada::getId).toList());
            for (FichaDeParticipante ficha : fichasDosJogadores) {
                alvo.adicionarJogador(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(),
                        ficha.modificadorBaseDeAtributo(Atributo.DESTREZA), ficha.bonusIniciativa());
            }
            fichasDosGrupos.forEach((grupo, ficha) -> adicionarGrupo(alvo, grupo, ficha));
            alvo.entradasEmOrdem().stream()
                    .filter(entrada -> !anteriores.contains(entrada.getId()))
                    .filter(entrada -> !entrada.isOculto())
                    .map(Combate.Entrada::getNomePersonagem)
                    .forEach(nomes::add);
        });
        return new AdicaoAoCombate(new EntradaNoCombateDto(combate.getId(), combate.getNome(), List.copyOf(nomes)),
                Set.copyOf(jogadoresComPersonagem.keySet()));
    }

    public CombateDto rolarIniciativa(MesaAtiva mesa, AcessoMesaDto jogador, ModoRolagem modo) {
        exigirMesaEmAndamento(mesa);
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;

        return mesa.combate().alterarDoJogador(jogador.userId(), combate -> {
            String entradaId = Combate.idDoJogador(jogador.userId());
            Combate.Entrada entrada = combate.entrada(entradaId);
            combate.exigirAguardandoRolagem(entradaId);
            combate.registrarIniciativa(entradaId, rolar(entrada, modoEscolhido));
        });
    }

    public CombateDto rolarIniciativaDosMonstros(MesaAtiva mesa, AcessoMesaDto mestre, String combateId,
                                                 List<String> entradaIds, ModoRolagem modo) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;

        return mesa.combate().alterar(combateId, combate -> {
            List<Combate.Entrada> monstros = monstrosParaRolar(combate, entradaIds);
            for (Combate.Entrada monstro : monstros) {
                combate.registrarIniciativa(monstro.getId(), rolar(monstro, modoEscolhido));
            }
        });
    }

    public CombateDto liberarRerolagem(MesaAtiva mesa, AcessoMesaDto mestre, String combateId,
                                       List<String> entradaIds) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<String> escolhidas = validarEntradas(entradaIds);
        return mesa.combate().alterar(combateId, combate -> combate.liberarRerolagem(escolhidas));
    }

    public CombateDto passarTurno(MesaAtiva mesa, AcessoMesaDto acesso, String combateId) {
        exigirMesaEmAndamento(mesa);
        if (ehMestre(acesso)) {
            return mesa.combate().alterar(combateId, Combate::passarTurno);
        }
        return mesa.combate().alterarDoJogador(acesso.userId(), combate -> {
            if (!acesso.userId().equals(combate.controladorDoTurno())) {
                throw new AccessDeniedException("Só quem está no turno, ou o mestre, pode passar o turno.");
            }
            combate.passarTurno();
        });
    }

    public EntradaNoCombateDto revelar(MesaAtiva mesa, AcessoMesaDto mestre, String combateId,
                                       List<String> entradaIds) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<String> escolhidas = validarEntradas(entradaIds);
        List<String> nomes = new ArrayList<>();
        CombateDto combate = mesa.combate().alterar(combateId, alvo -> nomes.addAll(alvo.revelar(escolhidas)));
        return new EntradaNoCombateDto(combate.id(), combate.nome(), List.copyOf(nomes));
    }

    public ReacaoDeclarada reagir(MesaAtiva mesa, AcessoMesaDto acesso, String combateId, String entradaId,
                                  Combate.TipoReacao tipo, String descricao) {
        exigirMesaEmAndamento(mesa);
        Combate.Reacao reacao = validarReacao(tipo, descricao);
        if (ehMestre(acesso)) {
            String monstroId = exigirMonstroInformado(entradaId);
            return mesa.combate().consultar(combateId, combate -> {
                exigirMonstro(combate.entrada(monstroId));
                combate.declararReacao(monstroId, reacao);
                return new ReacaoDeclarada(combate.getId(), combate.entrada(monstroId).getNomePersonagem(), reacao);
            });
        }
        String proprioId = Combate.idDoJogador(acesso.userId());
        return mesa.combate().consultarDoJogador(acesso.userId(), combate -> {
            combate.declararReacao(proprioId, reacao);
            return new ReacaoDeclarada(combate.getId(), combate.entrada(proprioId).getNomePersonagem(), reacao);
        });
    }

    public void encerrarReacao(MesaAtiva mesa, AcessoMesaDto acesso, String combateId, String entradaId) {
        if (ehMestre(acesso)) {
            String monstroId = exigirMonstroInformado(entradaId);
            mesa.combate().alterar(combateId, combate -> {
                exigirMonstro(combate.entrada(monstroId));
                combate.encerrarReacao(monstroId);
            });
            return;
        }
        mesa.combate().alterarDoJogador(acesso.userId(),
                combate -> combate.encerrarReacao(Combate.idDoJogador(acesso.userId())));
    }

    public Optional<ReacaoDeclarada> reacaoAbertaDe(MesaAtiva mesa, Long userId) {
        return mesa.combate().reacaoAbertaDe(userId);
    }

    public void encerrar(MesaAtiva mesa, AcessoMesaDto mestre, String combateId) {
        exigirMestre(mestre);
        mesa.combate().encerrar(combateId);
    }

    public List<CombateDto> estados(MesaAtiva mesa) {
        return mesa.combate().estados();
    }

    public CombatesDaMesaDto visaoDo(MesaAtiva mesa, AcessoMesaDto acesso) {
        return mesa.combate().visaoDo(acesso.userId(), ehMestre(acesso));
    }

    public Set<String> combatesAtivos(MesaAtiva mesa) {
        return mesa.combate().ids();
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        mesa.combate().removerJogador(jogadorId);
    }

    public boolean podeRolarLivremente(MesaAtiva mesa, Long userId) {
        return mesa.combate().podeRolarLivremente(userId);
    }

    private FichaDeParticipante fichaDoGrupo(MesaAtiva mesa, AcessoMesaDto mestre, GrupoDeMonstros grupo) {
        return mesaService.buscarFichaDeNpcDaMesa(mesa.getMesaId(), grupo.personagemId(), mestre.userId());
    }

    private void adicionarGrupo(Combate combate, GrupoDeMonstros grupo, FichaDeParticipante ficha) {
        int destreza = ficha.modificadorBaseDeAtributo(Atributo.DESTREZA);
        String nomeBase = grupo.nome() == null || grupo.nome().isBlank() ? ficha.nomePersonagem() : grupo.nome().trim();
        long existentes = combate.monstrosDoGrupo(nomeBase);
        for (int numero = 1; numero <= grupo.quantidade(); numero++) {
            boolean unico = existentes == 0 && grupo.quantidade() == 1;
            String nome = unico ? nomeBase : nomeBase + " " + (existentes + numero);
            combate.adicionarMonstro(ficha.personagemId(), nomeBase, nome, ficha.nomeJogador(),
                    destreza, ficha.bonusIniciativa(), grupo.ehOculto());
        }
    }

    private List<Combate.Entrada> monstrosParaRolar(Combate combate, List<String> entradaIds) {
        List<Combate.Entrada> escolhidos = entradaIds == null || entradaIds.isEmpty()
                ? combate.entradasEmOrdem().stream()
                .filter(entrada -> entrada.getTipo() == Combate.TipoEntrada.MONSTRO)
                .filter(Combate.Entrada::aguardandoRolagem)
                .toList()
                : validarEntradas(entradaIds).stream().map(combate::entrada).toList();

        if (escolhidos.isEmpty()) {
            throw new IllegalStateException("Nenhum monstro aguarda rolagem de iniciativa.");
        }
        for (Combate.Entrada entrada : escolhidos) {
            if (entrada.getTipo() != Combate.TipoEntrada.MONSTRO) {
                throw new IllegalStateException("O mestre só rola a iniciativa dos monstros, NPCs e chefes.");
            }
            combate.exigirAguardandoRolagem(entrada.getId());
        }
        return escolhidos;
    }

    private Iniciativa rolar(Combate.Entrada entrada, ModoRolagem modo) {
        DiceRollResult dados = rolagemDados.rolar(new PedidoRolagem(20, 1, 0, modo));
        int total = dados.getKept() + entrada.getModificadorDestreza() + entrada.getBonusIniciativa();
        return new Iniciativa(dados.getRolls(), dados.getKept(), entrada.getModificadorDestreza(),
                entrada.getBonusIniciativa(), total, modo);
    }

    private Combate.Reacao validarReacao(Combate.TipoReacao tipo, String descricao) {
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo da reação.");
        }
        String limpa = descricao == null || descricao.isBlank() ? null : descricao.trim();
        if (tipo == Combate.TipoReacao.OUTRA && limpa == null) {
            throw new IllegalArgumentException("Descreva a reação.");
        }
        if (limpa != null && limpa.length() > MAXIMO_CARACTERES_DA_REACAO) {
            throw new IllegalArgumentException(
                    "A descrição da reação deve ter até " + MAXIMO_CARACTERES_DA_REACAO + " caracteres.");
        }
        return new Combate.Reacao(tipo, limpa);
    }

    private String exigirMonstroInformado(String entradaId) {
        if (entradaId == null || entradaId.isBlank()) {
            throw new IllegalArgumentException("Escolha o monstro que vai reagir.");
        }
        return entradaId;
    }

    private void exigirMonstro(Combate.Entrada entrada) {
        if (entrada.getTipo() != Combate.TipoEntrada.MONSTRO) {
            throw new AccessDeniedException("O mestre só declara reações dos monstros, NPCs e chefes.");
        }
    }

    private boolean ehMestre(AcessoMesaDto acesso) {
        return acesso != null && "MESTRE".equals(acesso.role());
    }

    private String validarNomeDoCombate(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String limpo = nome.trim();
        if (limpo.length() > MAXIMO_CARACTERES_DO_NOME) {
            throw new IllegalArgumentException(
                    "O nome do combate deve ter até " + MAXIMO_CARACTERES_DO_NOME + " caracteres.");
        }
        return limpo;
    }

    private void exigirMestre(AcessoMesaDto acesso) {
        if (!ehMestre(acesso)) {
            throw new AccessDeniedException("Apenas o mestre pode administrar o combate.");
        }
    }

    private void exigirMesaEmAndamento(MesaAtiva mesa) {
        if (mesa.pausada()) {
            throw new IllegalStateException("A mesa está pausada. O combate fica congelado até a mesa voltar.");
        }
    }

    private List<Long> validarJogadores(List<Long> jogadorIds) {
        if (jogadorIds == null || jogadorIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um jogador para o combate.");
        }
        Set<Long> vistos = new HashSet<>();
        for (Long jogadorId : jogadorIds) {
            if (jogadorId == null) {
                throw new IllegalArgumentException("Jogador do combate não informado.");
            }
            if (!vistos.add(jogadorId)) {
                throw new IllegalArgumentException("Cada jogador só pode aparecer uma vez.");
            }
        }
        return jogadorIds;
    }

    private List<GrupoDeMonstros> validarGrupos(List<GrupoDeMonstros> grupos) {
        if (grupos == null) {
            return List.of();
        }
        Set<Long> vistos = new HashSet<>();
        for (GrupoDeMonstros grupo : grupos) {
            if (grupo == null || grupo.personagemId() == null) {
                throw new IllegalArgumentException("Ficha do monstro não informada.");
            }
            if (!vistos.add(grupo.personagemId())) {
                throw new IllegalArgumentException("Cada ficha só pode aparecer uma vez. Use a quantidade para replicar.");
            }
            if (grupo.nome() != null && grupo.nome().trim().length() > MAXIMO_CARACTERES_DO_NOME) {
                throw new IllegalArgumentException(
                        "O nome do monstro deve ter até " + MAXIMO_CARACTERES_DO_NOME + " caracteres.");
            }
            if (grupo.quantidade() == null || grupo.quantidade() < 1
                    || grupo.quantidade() > MAXIMO_MONSTROS_POR_GRUPO) {
                throw new IllegalArgumentException(
                        "A quantidade de monstros deve ficar entre 1 e " + MAXIMO_MONSTROS_POR_GRUPO + ".");
            }
        }
        return grupos;
    }

    private List<String> validarEntradas(List<String> entradaIds) {
        if (entradaIds == null || entradaIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um participante do combate.");
        }
        Set<String> vistos = new HashSet<>();
        for (String entradaId : entradaIds) {
            if (entradaId == null) {
                throw new IllegalArgumentException("Participante do combate não informado.");
            }
            if (!vistos.add(entradaId)) {
                throw new IllegalArgumentException("Cada participante só pode aparecer uma vez.");
            }
        }
        return entradaIds;
    }
}
