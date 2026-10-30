package com.arcana.backend.chat.desafio;

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

    public DesafioDto criar(MesaAtiva mesa, AcessoMesaDto mestre, NovoDesafio pedido) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido de desafio ausente.");
        }
        int cd = validarCd(pedido.cd());
        String descricao = validarDescricao(pedido.descricao());
        List<NovoDesafio.Convocacao> convocacoes = validarConvocacoes(pedido.jogadores());

        Desafio desafio = new Desafio(UUID.randomUUID().toString(), mestre.userId(), cd, descricao, clock.instant());
        for (NovoDesafio.Convocacao convocacao : convocacoes) {
            FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), convocacao.jogadorId());
            ModoRolagem modo = convocacao.modo() != null ? convocacao.modo() : ModoRolagem.NORMAL;
            desafio.convocar(ficha.jogadorId(), ficha.nomeJogador(), ficha.nomePersonagem(), modo);
        }
        return mesa.desafios().adicionar(desafio);
    }

    public ResultadoRolagem rolar(MesaAtiva mesa, AcessoMesaDto jogador, EscolhaTeste escolha) {
        exigirMesaEmAndamento(mesa);
        if (escolha == null) {
            throw new IllegalArgumentException("Escolha um atributo ou uma perícia para o teste.");
        }
        Long jogadorId = jogador.userId();
        DesafioDto ativo = mesa.desafios().ativoDoJogador(jogadorId)
                .orElseThrow(() -> new IllegalStateException("Você não tem um desafio em andamento."));

        FichaDeParticipante ficha = mesaService.buscarFichaDoJogadorNaMesa(mesa.getMesaId(), jogadorId);
        int modificador = escolha.modificadorEm(ficha);

        RolagemDeDesafio[] feita = new RolagemDeDesafio[1];
        DesafioDto atualizado = mesa.desafios().alterar(ativo.id(), jogadorId, desafio -> {
            desafio.validarRolagem(jogadorId);
            Desafio.Participacao participacao = desafio.participacao(jogadorId);
            DiceRollResult dados = rolarD20(participacao.getModo(), modificador, escolha);
            int total = dados.getTotal();
            feita[0] = new RolagemDeDesafio(participacao.proximaTentativa(), escolha, escolha.descricao(),
                    dados, total, total >= desafio.getCd());
            desafio.registrarRolagem(jogadorId, feita[0]);
        });
        return new ResultadoRolagem(atualizado, feita[0]);
    }

    public DesafioDto liberarTentativaExtra(MesaAtiva mesa, AcessoMesaDto mestre, String desafioId,
                                            Long jogadorId, ModoRolagem modo) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        if (modo == null) {
            throw new IllegalArgumentException("Defina se a nova tentativa será normal, com vantagem ou com desvantagem.");
        }
        return mesa.desafios().alterar(desafioId, mestre.userId(), desafio -> {
            exigirDonoDoDesafio(desafio, mestre);
            desafio.liberarTentativaExtra(jogadorId, modo);
        });
    }

    public DesafioDto encerrarComFalha(MesaAtiva mesa, AcessoMesaDto mestre, String desafioId, Long jogadorId) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        return mesa.desafios().alterar(desafioId, mestre.userId(), desafio -> {
            exigirDonoDoDesafio(desafio, mestre);
            desafio.encerrarComFalha(jogadorId);
        });
    }

    public DesafioDto cancelar(MesaAtiva mesa, AcessoMesaDto mestre, String desafioId, Long jogadorIdOuTodos) {
        exigirMestre(mestre);
        return mesa.desafios().alterar(desafioId, mestre.userId(), desafio -> {
            exigirDonoDoDesafio(desafio, mestre);
            boolean cancelou = jogadorIdOuTodos == null
                    ? desafio.cancelarTodos()
                    : desafio.cancelar(jogadorIdOuTodos);
            if (!cancelou) {
                throw new IllegalStateException("Não há desafio em andamento para cancelar.");
            }
        });
    }

    public MensagemEnviada enviarMensagem(MesaAtiva mesa, AcessoMesaDto mestre, String desafioId,
                                          MensagemDeResultado.Destino destino, Long jogadorId, String texto) {
        exigirMestre(mestre);
        exigirMesaEmAndamento(mesa);
        if (destino == null) {
            throw new IllegalArgumentException("Escolha para quem a mensagem será enviada.");
        }
        String limpo = validarTextoDaMensagem(texto);

        MensagemDeResultado[] enviada = new MensagemDeResultado[1];
        DesafioDto atualizado = mesa.desafios().alterar(desafioId, mestre.userId(), desafio -> {
            exigirDonoDoDesafio(desafio, mestre);
            enviada[0] = new MensagemDeResultado(UUID.randomUUID().toString(), limpo, destino,
                    desafio.destinatariosDaMensagem(destino, jogadorId), clock.instant());
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

    public List<DesafioDto> cancelarPorAusenciaDoMestre(MesaAtiva mesa) {
        return mesa.desafios().cancelarTodos();
    }

    public boolean jogadorBloqueado(MesaAtiva mesa, Long jogadorId) {
        return mesa.desafios().jogadorEmDesafioAtivo(jogadorId);
    }

    public List<DesafioDto> visiveisPara(MesaAtiva mesa, Long userId) {
        return mesa.desafios().visiveisPara(userId);
    }

    private DiceRollResult rolarD20(ModoRolagem modo, int modificador, EscolhaTeste escolha) {
        DiceRollResult dados = rolagemDados.rolar(new PedidoRolagem(20, 1, 0, modo));
        dados.setModifier(modificador);
        dados.setTotal(dados.getKept() + modificador);
        dados.setExpression(modificador == 0 ? "1d20" : "1d20" + (modificador > 0 ? "+" : "") + modificador);
        dados.setAttributeUsed(escolha.descricao());
        return dados;
    }

    private void exigirMestre(AcessoMesaDto acesso) {
        if (acesso == null || !"MESTRE".equals(acesso.role())) {
            throw new AccessDeniedException("Apenas o mestre pode administrar desafios.");
        }
    }

    private void exigirDonoDoDesafio(Desafio desafio, AcessoMesaDto mestre) {
        if (!desafio.getMestreId().equals(mestre.userId())) {
            throw new AccessDeniedException("Apenas o mestre que criou o desafio pode alterá-lo.");
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
        Set<Long> vistos = new HashSet<>();
        for (NovoDesafio.Convocacao convocacao : convocacoes) {
            if (convocacao == null || convocacao.jogadorId() == null) {
                throw new IllegalArgumentException("Jogador do desafio não informado.");
            }
            if (!vistos.add(convocacao.jogadorId())) {
                throw new IllegalArgumentException("Cada jogador só pode aparecer uma vez no desafio.");
            }
        }
        return convocacoes;
    }
}
