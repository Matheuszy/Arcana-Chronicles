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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CombateService {

    static final int MAXIMO_MONSTROS_POR_GRUPO = 20;
    static final int MAXIMO_CARACTERES_DO_NOME = 60;

    private final MesaService mesaService;
    private final RolagemDadosService rolagemDados;
    private final Clock clock;

    public CombateService(MesaService mesaService, RolagemDadosService rolagemDados, Clock clock) {
        this.mesaService = mesaService;
        this.rolagemDados = rolagemDados;
        this.clock = clock;
    }

    public CombateDto iniciar(MesaAtiva mesa, AcessoMesaDto mestre, List<Long> jogadorIds,
                              List<GrupoDeMonstros> grupos) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<Long> escolhidos = validarJogadores(jogadorIds);
        List<GrupoDeMonstros> gruposEscolhidos = validarGrupos(grupos);

        Combate combate = new Combate(UUID.randomUUID().toString(), mestre.userId(), clock.instant());
        for (Long jogadorId : escolhidos) {
            FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId);
            combate.adicionarJogador(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(),
                    ficha.modificadorBaseDeAtributo(Atributo.DESTREZA), ficha.bonusProficiencia());
        }
        for (GrupoDeMonstros grupo : gruposEscolhidos) {
            adicionarGrupo(combate, mesa, mestre, grupo);
        }
        return mesa.combate().iniciar(combate);
    }

    public CombateDto rolarIniciativa(MesaAtiva mesa, AcessoMesaDto jogador, ModoRolagem modo,
                                      Boolean somarProficiencia) {
        exigirMesaEmAndamento(mesa);
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;
        boolean somar = Boolean.TRUE.equals(somarProficiencia);

        return mesa.combate().alterar(combate -> {
            String entradaId = Combate.idDoJogador(jogador.userId());
            Combate.Entrada entrada = combate.entrada(entradaId);
            combate.exigirAguardandoRolagem(entradaId);
            combate.registrarIniciativa(entradaId, rolar(entrada, modoEscolhido, somar));
        });
    }

    public CombateDto rolarIniciativaDosMonstros(MesaAtiva mesa, AcessoMesaDto mestre, List<String> entradaIds,
                                                 ModoRolagem modo, Boolean somarProficiencia) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;
        boolean somar = Boolean.TRUE.equals(somarProficiencia);

        return mesa.combate().alterar(combate -> {
            List<Combate.Entrada> monstros = monstrosParaRolar(combate, entradaIds);
            for (Combate.Entrada monstro : monstros) {
                combate.registrarIniciativa(monstro.getId(), rolar(monstro, modoEscolhido, somar));
            }
        });
    }

    public CombateDto liberarRerolagem(MesaAtiva mesa, AcessoMesaDto mestre, List<String> entradaIds) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<String> escolhidas = validarEntradas(entradaIds);
        return mesa.combate().alterar(combate -> combate.liberarRerolagem(escolhidas));
    }

    public CombateDto passarTurno(MesaAtiva mesa, AcessoMesaDto acesso) {
        exigirMesaEmAndamento(mesa);
        return mesa.combate().alterar(combate -> {
            boolean ehMestre = "MESTRE".equals(acesso.role());
            if (!ehMestre && !acesso.userId().equals(combate.controladorDoTurno())) {
                throw new AccessDeniedException("Só quem está no turno, ou o mestre, pode passar o turno.");
            }
            combate.passarTurno();
        });
    }

    public void encerrar(MesaAtiva mesa, AcessoMesaDto mestre) {
        exigirMestre(mestre);
        mesa.combate().encerrar();
    }

    public Optional<CombateDto> estado(MesaAtiva mesa) {
        return mesa.combate().estado();
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        mesa.combate().removerJogador(jogadorId);
    }

    public boolean podeRolarLivremente(MesaAtiva mesa, Long userId) {
        return mesa.combate().podeRolarLivremente(userId);
    }

    private void adicionarGrupo(Combate combate, MesaAtiva mesa, AcessoMesaDto mestre, GrupoDeMonstros grupo) {
        FichaDeParticipante ficha = mesaService.buscarFichaDeNpcDaMesa(mesa.getMesaId(), grupo.personagemId(),
                mestre.userId());
        int destreza = ficha.modificadorBaseDeAtributo(Atributo.DESTREZA);
        String nomeBase = grupo.nome() == null || grupo.nome().isBlank() ? ficha.nomePersonagem() : grupo.nome().trim();
        for (int numero = 1; numero <= grupo.quantidade(); numero++) {
            String nome = grupo.quantidade() == 1 ? nomeBase : nomeBase + " " + numero;
            combate.adicionarMonstro(ficha.personagemId(), nomeBase, nome, ficha.nomeJogador(),
                    destreza, ficha.bonusProficiencia());
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

    private Iniciativa rolar(Combate.Entrada entrada, ModoRolagem modo, boolean somarProficiencia) {
        int bonus = somarProficiencia ? entrada.getBonusProficiencia() : 0;
        DiceRollResult dados = rolagemDados.rolar(new PedidoRolagem(20, 1, 0, modo));
        int total = dados.getKept() + entrada.getModificadorDestreza() + bonus;
        return new Iniciativa(dados.getRolls(), dados.getKept(), entrada.getModificadorDestreza(), bonus, total,
                modo, somarProficiencia);
    }

    private void exigirMestre(AcessoMesaDto acesso) {
        if (acesso == null || !"MESTRE".equals(acesso.role())) {
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
