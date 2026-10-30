package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.combate.Combate;
import com.arcana.backend.chat.combate.CombateDto;
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
import java.util.Set;
import java.util.UUID;

@Service
public class DesafioService {

    public static final int CD_MINIMA = 1;
    public static final int CD_MAXIMA = 40;
    public static final int LIMITE_DESCRICAO = 120;

    public static final int LIMITE_MENSAGEM = 120;

    public record ResultadoRolagem(DesafioDto desafio, RolagemDeDesafio rolagem) {}

    public record MensagemEnviada(DesafioDto desafio, MensagemDeResultado mensagem) {}

    private final MesaService mesaService;
    private final RolagemDadosService rolagemDados;
    private final Clock clock;

    public DesafioService(MesaService mesaService, RolagemDadosService rolagemDados, Clock clock) {
        this.mesaService = mesaService;
        this.rolagemDados = rolagemDados;
        this.clock = clock;
    }

    public DesafioDto criar(MesaAtiva mesa, AcessoMesaDto criador, NovoDesafio pedido) {
        if (criador == null) {
            throw new AccessDeniedException("Apenas o mestre pode administrar desafios.");
        }
        boolean ehMestre = "MESTRE".equals(criador.role());
        CombateDto combate = mesa.combate().estado().orElse(null);
        if (!ehMestre) {
            exigirJogadorNoTurnoDoCombate(combate, criador);
        }
        exigirMesaEmAndamento(mesa);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido de desafio ausente.");
        }
        int cd = validarCd(pedido.cd());
        String descricao = validarDescricao(pedido.descricao());
        List<NovoDesafio.Convocacao> convocacoes = validarConvocacoes(pedido.jogadores());

        Desafio desafio = new Desafio(UUID.randomUUID().toString(), criador.userId(), criador.displayName(),
                !ehMestre, cd, descricao, clock.instant());
        if (ehMestre) {
            convocarJogadoresDaMesa(desafio, mesa, convocacoes);
        } else {
            convocarParticipantesDoCombate(desafio, combate, criador, convocacoes);
        }
        return mesa.desafios().adicionar(desafio);
    }

    public ResultadoRolagem rolar(MesaAtiva mesa, AcessoMesaDto jogador, EscolhaTeste escolha) {
        exigirMesaEmAndamento(mesa);
        if (escolha == null) {
            throw new IllegalArgumentException("Escolha um atributo ou uma perícia para o teste.");
        }
        Long jogadorId = jogador.userId();
        String alvoId = Desafio.idDoJogador(jogadorId);
        DesafioDto ativo = mesa.desafios().ativoDoJogador(jogadorId)
                .orElseThrow(() -> new IllegalStateException("Você não tem um desafio em andamento."));

        FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId);
        int modificador = escolha.modificadorEm(ficha);

        RolagemDeDesafio[] feita = new RolagemDeDesafio[1];
        DesafioDto atualizado = mesa.desafios().alterar(ativo.id(), jogadorId, desafio -> {
            desafio.validarRolagem(alvoId);
            Desafio.Participacao participacao = desafio.participacao(alvoId);
            feita[0] = rolarParticipacao(desafio, participacao, participacao.getModo(), modificador, escolha);
        });
        return new ResultadoRolagem(atualizado, feita[0]);
    }

    public DesafioDto rolarMonstros(MesaAtiva mesa, AcessoMesaDto mestre, String desafioId, List<String> alvoIds,
                                    EscolhaTeste escolha, ModoRolagem modo) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        if (escolha == null) {
            throw new IllegalArgumentException("Escolha um atributo ou uma perícia para o teste.");
        }
        ModoRolagem modoEscolhido = modo != null ? modo : ModoRolagem.NORMAL;

        return mesa.desafios().alterar(desafioId, mestre.userId(), desafio -> {
            if (!desafio.envolve(mestre.userId())) {
                throw new IllegalArgumentException("Desafio não encontrado.");
            }
            List<Desafio.Participacao> monstros = monstrosParaRolar(desafio, mestre, alvoIds);
            for (Desafio.Participacao monstro : monstros) {
                FichaDeParticipante ficha = mesaService.buscarFichaDeNpcDaMesa(mesa.getMesaId(),
                        monstro.getPersonagemId(), mestre.userId());
                monstro.definirModo(modoEscolhido);
                rolarParticipacao(desafio, monstro, modoEscolhido, escolha.modificadorEm(ficha), escolha);
            }
        });
    }

    public DesafioDto liberarTentativaExtra(MesaAtiva mesa, AcessoMesaDto criador, String desafioId,
                                            String alvoId, ModoRolagem modo) {
        exigirMesaEmAndamento(mesa);
        if (modo == null) {
            throw new IllegalArgumentException("Defina se a nova tentativa será normal, com vantagem ou com desvantagem.");
        }
        return mesa.desafios().alterar(desafioId, criador.userId(), desafio -> {
            exigirCriadorDoDesafio(desafio, criador);
            desafio.liberarTentativaExtra(alvoId, modo);
        });
    }

    public DesafioDto encerrarComFalha(MesaAtiva mesa, AcessoMesaDto criador, String desafioId, String alvoId) {
        exigirMesaEmAndamento(mesa);
        return mesa.desafios().alterar(desafioId, criador.userId(), desafio -> {
            exigirCriadorDoDesafio(desafio, criador);
            desafio.encerrarComFalha(alvoId);
        });
    }

    public DesafioDto cancelar(MesaAtiva mesa, AcessoMesaDto criador, String desafioId, String alvoIdOuTodos) {
        return mesa.desafios().alterar(desafioId, criador.userId(), desafio -> {
            exigirCriadorDoDesafio(desafio, criador);
            boolean cancelou = alvoIdOuTodos == null
                    ? desafio.cancelarTodos()
                    : desafio.cancelar(alvoIdOuTodos);
            if (!cancelou) {
                throw new IllegalStateException("Não há desafio em andamento para cancelar.");
            }
        });
    }

    public MensagemEnviada enviarMensagem(MesaAtiva mesa, AcessoMesaDto criador, String desafioId,
                                          MensagemDeResultado.Destino destino, String alvoId, String texto) {
        exigirMesaEmAndamento(mesa);
        if (destino == null) {
            throw new IllegalArgumentException("Escolha para quem a mensagem será enviada.");
        }
        String limpo = validarTextoDaMensagem(texto);

        MensagemDeResultado[] enviada = new MensagemDeResultado[1];
        DesafioDto atualizado = mesa.desafios().alterar(desafioId, criador.userId(), desafio -> {
            exigirCriadorDoDesafio(desafio, criador);
            enviada[0] = new MensagemDeResultado(UUID.randomUUID().toString(), limpo, destino,
                    desafio.destinatariosDaMensagem(destino, alvoId), clock.instant());
            desafio.registrarMensagem(enviada[0]);
        });
        return new MensagemEnviada(atualizado, enviada[0]);
    }

    public DesafioDto visaoPara(MesaAtiva mesa, String desafioId, Long userId) {
        return mesa.desafios().visaoPara(desafioId, userId);
    }

    public Set<Long> envolvidos(MesaAtiva mesa, String desafioId) {
        return mesa.desafios().envolvidos(desafioId);
    }

    public List<DesafioDto> cancelarDoJogador(MesaAtiva mesa, Long jogadorId) {
        return mesa.desafios().cancelarDoJogador(jogadorId);
    }

    public List<DesafioDto> cancelarDoCombate(MesaAtiva mesa) {
        return mesa.desafios().cancelarDoCombate();
    }

    public List<DesafioDto> cancelarPorAusenciaDoMestre(MesaAtiva mesa) {
        return mesa.desafios().cancelarTodos();
    }

    public boolean jogadorBloqueado(MesaAtiva mesa, Long jogadorId) {
        return mesa.desafios().jogadorEmDesafioAtivo(jogadorId);
    }

    public List<DesafioDto> visiveisPara(MesaAtiva mesa, Long userId) {
        return mesa.desafios().visiveisPara(userId);
    }

    private void convocarJogadoresDaMesa(Desafio desafio, MesaAtiva mesa, List<NovoDesafio.Convocacao> convocacoes) {
        for (NovoDesafio.Convocacao convocacao : convocacoes) {
            FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(),
                    jogadorDoAlvo(convocacao.alvo()));
            ModoRolagem modo = convocacao.modo() != null ? convocacao.modo() : ModoRolagem.NORMAL;
            desafio.convocarJogador(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(), modo);
        }
    }

    private void convocarParticipantesDoCombate(Desafio desafio, CombateDto combate, AcessoMesaDto criador,
                                                List<NovoDesafio.Convocacao> convocacoes) {
        String proprioId = Desafio.idDoJogador(criador.userId());
        for (NovoDesafio.Convocacao convocacao : convocacoes) {
            String alvoId = convocacao.alvo();
            if (proprioId.equals(alvoId)) {
                throw new IllegalArgumentException("Você não pode desafiar a si mesmo.");
            }
            CombateDto.EntradaDto alvo = combate.participantes().stream()
                    .filter(entrada -> entrada.id().equals(alvoId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Este alvo não participa do combate."));
            if (alvo.tipo() == Combate.TipoEntrada.JOGADOR) {
                ModoRolagem modo = convocacao.modo() != null ? convocacao.modo() : ModoRolagem.NORMAL;
                desafio.convocarJogador(alvo.controladorId(), alvo.nomeJogador(), alvo.nomePersonagem(), modo);
            } else {
                desafio.convocarMonstro(alvo.id(), alvo.controladorId(), alvo.personagemId(), alvo.nomeJogador(),
                        alvo.nomePersonagem());
            }
        }
    }

    private void exigirJogadorNoTurnoDoCombate(CombateDto combate, AcessoMesaDto jogador) {
        if (combate == null) {
            throw new AccessDeniedException("Jogadores só podem desafiar durante o combate.");
        }
        if (combate.fase() != Combate.Fase.EM_ANDAMENTO) {
            throw new IllegalStateException("O combate só começa depois que todos rolarem a iniciativa.");
        }
        if (!jogador.userId().equals(combate.controladorDoTurno())) {
            throw new AccessDeniedException("Só é possível desafiar no seu turno.");
        }
    }

    private List<Desafio.Participacao> monstrosParaRolar(Desafio desafio, AcessoMesaDto mestre, List<String> alvoIds) {
        List<Desafio.Participacao> escolhidos = alvoIds == null || alvoIds.isEmpty()
                ? desafio.participacoes().stream()
                .filter(p -> p.getTipo() == Desafio.TipoAlvo.MONSTRO)
                .filter(p -> p.getEstado().aguardandoRolagem())
                .toList()
                : alvoIds.stream().map(desafio::participacao).toList();

        if (escolhidos.isEmpty()) {
            throw new IllegalStateException("Nenhum monstro aguarda rolagem neste desafio.");
        }
        for (Desafio.Participacao participacao : escolhidos) {
            if (participacao.getTipo() != Desafio.TipoAlvo.MONSTRO
                    || !participacao.getControladorId().equals(mestre.userId())) {
                throw new AccessDeniedException("O mestre só rola por monstros, NPCs e chefes.");
            }
            desafio.validarRolagem(participacao.getId());
        }
        return escolhidos;
    }

    private RolagemDeDesafio rolarParticipacao(Desafio desafio, Desafio.Participacao participacao, ModoRolagem modo,
                                               int modificador, EscolhaTeste escolha) {
        DiceRollResult dados = rolarD20(modo, modificador, escolha);
        int total = dados.getTotal();
        RolagemDeDesafio rolagem = new RolagemDeDesafio(participacao.proximaTentativa(), escolha,
                escolha.descricao(), dados, total, total >= desafio.getCd());
        desafio.registrarRolagem(participacao.getId(), rolagem);
        return rolagem;
    }

    private DiceRollResult rolarD20(ModoRolagem modo, int modificador, EscolhaTeste escolha) {
        DiceRollResult dados = rolagemDados.rolar(new PedidoRolagem(20, 1, 0, modo));
        dados.setModifier(modificador);
        dados.setTotal(dados.getKept() + modificador);
        dados.setExpression(modificador == 0 ? "1d20" : "1d20" + (modificador > 0 ? "+" : "") + modificador);
        dados.setAttributeUsed(escolha.descricao());
        return dados;
    }

    private Long jogadorDoAlvo(String alvoId) {
        try {
            return Long.valueOf(alvoId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Jogador do desafio inválido.");
        }
    }

    private void exigirMestre(AcessoMesaDto acesso) {
        if (acesso == null || !"MESTRE".equals(acesso.role())) {
            throw new AccessDeniedException("Apenas o mestre pode administrar desafios.");
        }
    }

    private void exigirCriadorDoDesafio(Desafio desafio, AcessoMesaDto acesso) {
        if (acesso == null) {
            throw new AccessDeniedException("Apenas quem criou o desafio pode alterá-lo.");
        }
        if (!desafio.getCriadorId().equals(acesso.userId())) {
            throw new AccessDeniedException(desafio.isDoCombate()
                    ? "Apenas quem criou o desafio pode alterá-lo."
                    : "Apenas o mestre pode administrar desafios.");
        }
    }

    private void exigirMesaEmAndamento(MesaAtiva mesa) {
        if (mesa.pausada()) {
            throw new IllegalStateException("A mesa está pausada. Os desafios ficam congelados até a mesa voltar.");
        }
    }

    private int validarCd(Integer cd) {
        if (cd == null || cd < CD_MINIMA || cd > CD_MAXIMA) {
            throw new IllegalArgumentException("A CD deve estar entre " + CD_MINIMA + " e " + CD_MAXIMA + ".");
        }
        return cd;
    }

    private String validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String limpa = descricao.trim();
        if (limpa.length() > LIMITE_DESCRICAO) {
            throw new IllegalArgumentException("A descrição do desafio pode ter no máximo " + LIMITE_DESCRICAO + " caracteres.");
        }
        return limpa;
    }

    private String validarTextoDaMensagem(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escreva a mensagem antes de enviar.");
        }
        String limpo = texto.trim();
        if (limpo.length() > LIMITE_MENSAGEM) {
            throw new IllegalArgumentException("A mensagem pode ter no máximo " + LIMITE_MENSAGEM + " caracteres.");
        }
        return limpo;
    }

    private List<NovoDesafio.Convocacao> validarConvocacoes(List<NovoDesafio.Convocacao> convocacoes) {
        if (convocacoes == null || convocacoes.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um jogador para o desafio.");
        }
        Set<String> vistos = new HashSet<>();
        for (NovoDesafio.Convocacao convocacao : convocacoes) {
            if (convocacao == null || convocacao.alvo() == null) {
                throw new IllegalArgumentException("Jogador do desafio não informado.");
            }
            if (!vistos.add(convocacao.alvo())) {
                throw new IllegalArgumentException("Cada jogador só pode aparecer uma vez no desafio.");
            }
        }
        return convocacoes;
    }
}
