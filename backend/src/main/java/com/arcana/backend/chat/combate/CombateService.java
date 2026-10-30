package com.arcana.backend.chat.combate;

import com.arcana.backend.character.model.Atributo;
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

    private final MesaService mesaService;
    private final RolagemDadosService rolagemDados;
    private final Clock clock;

    public CombateService(MesaService mesaService, RolagemDadosService rolagemDados, Clock clock) {
        this.mesaService = mesaService;
        this.rolagemDados = rolagemDados;
        this.clock = clock;
    }

    public CombateDto iniciar(MesaAtiva mesa, AcessoMesaDto mestre, List<Long> jogadorIds) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<Long> escolhidos = validarJogadores(jogadorIds);

        Combate combate = new Combate(UUID.randomUUID().toString(), mestre.userId(), clock.instant());
        for (Long jogadorId : escolhidos) {
            FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId);
            combate.adicionar(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(),
                    ficha.modificadorBaseDeAtributo(Atributo.DESTREZA), ficha.bonusProficiencia());
        }
        return mesa.combate().iniciar(combate);
    }

    public CombateDto rolarIniciativa(MesaAtiva mesa, AcessoMesaDto jogador, ModoRolagem modo,
                                      Boolean somarProficiencia) {
        exigirMesaEmAndamento(mesa);
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;
        boolean somar = Boolean.TRUE.equals(somarProficiencia);

        return mesa.combate().alterar(combate -> {
            Combate.Entrada entrada = combate.entrada(jogador.userId());
            combate.exigirAguardandoRolagem(jogador.userId());
            int bonus = somar ? entrada.getBonusProficiencia() : 0;
            DiceRollResult dados = rolagemDados.rolar(new PedidoRolagem(20, 1, 0, modoEscolhido));
            int total = dados.getKept() + entrada.getModificadorDestreza() + bonus;
            combate.registrarIniciativa(jogador.userId(), new Iniciativa(dados.getRolls(), dados.getKept(),
                    entrada.getModificadorDestreza(), bonus, total, modoEscolhido, somar));
        });
    }

    public CombateDto liberarRerolagem(MesaAtiva mesa, AcessoMesaDto mestre, List<Long> jogadorIds) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        List<Long> escolhidos = validarJogadores(jogadorIds);
        return mesa.combate().alterar(combate -> combate.liberarRerolagem(escolhidos));
    }

    public CombateDto passarTurno(MesaAtiva mesa, AcessoMesaDto acesso) {
        exigirMesaEmAndamento(mesa);
        return mesa.combate().alterar(combate -> {
            boolean ehMestre = "MESTRE".equals(acesso.role());
            if (!ehMestre && !acesso.userId().equals(combate.jogadorDoTurno())) {
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
}
