package com.arcana.backend.chat.sala;

import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.AlvoDeStatus;
import com.arcana.backend.table.service.MesaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class SalaService {

    private final MesaService mesaService;
    private final Clock clock;

    public SalaService(MesaService mesaService, Clock clock) {
        this.mesaService = mesaService;
        this.clock = clock;
    }

    public String salvarNota(MesaAtiva mesa, AcessoMesaDto acesso, String texto) {
        return mesa.notas().salvar(acesso.userId(), texto);
    }

    public String notaDe(MesaAtiva mesa, Long userId) {
        return mesa.notas().de(userId);
    }

    public StatusDto.Registro definirStatus(MesaAtiva mesa, AcessoMesaDto acesso, ComandoSala comando) {
        exigirMestre(acesso, "Apenas o mestre pode alterar status.");
        if (comando.personagemId() == null) {
            throw new IllegalArgumentException("Escolha a ficha que recebe o status.");
        }
        AlvoDeStatus alvo = mesaService.buscarAlvoDeStatus(mesa.getMesaId(), comando.personagemId());
        return mesa.statusDeFichas().definir(alvo, comando.texto(), clock.instant());
    }

    public StatusDto.Registro removerStatus(MesaAtiva mesa, AcessoMesaDto acesso, ComandoSala comando) {
        exigirMestre(acesso, "Apenas o mestre pode alterar status.");
        if (comando.personagemId() == null) {
            throw new IllegalArgumentException("Escolha a ficha que perde o status.");
        }
        return mesa.statusDeFichas().remover(comando.personagemId(), clock.instant());
    }

    public List<StatusDto.Registro> removerStatusDoJogador(MesaAtiva mesa, Long jogadorId) {
        return mesa.statusDeFichas().removerDoDono(jogadorId, clock.instant());
    }

    public StatusDto statusPara(MesaAtiva mesa, AcessoMesaDto acesso) {
        return mesa.statusDeFichas().visaoPara(ehMestre(acesso));
    }

    public ItemDto criarItem(MesaAtiva mesa, AcessoMesaDto acesso, ComandoSala comando) {
        if (mesa.pausada()) {
            throw new RegraDeNegocioException("A mesa está pausada. Itens não podem ser criados até o mestre retomar.");
        }
        return mesa.itens().adicionar(acesso.userId(), acesso.displayName(), comando.nome(), comando.descricao(),
                clock.instant());
    }

    public ItemDto removerItem(MesaAtiva mesa, AcessoMesaDto acesso, ComandoSala comando) {
        return mesa.itens().remover(comando.itemId(), acesso.userId(), ehMestre(acesso));
    }

    public List<ItemDto> itens(MesaAtiva mesa) {
        return mesa.itens().lista();
    }

    private void exigirMestre(AcessoMesaDto acesso, String mensagem) {
        if (!ehMestre(acesso)) {
            throw new AccessDeniedException(mensagem);
        }
    }

    private boolean ehMestre(AcessoMesaDto acesso) {
        return "MESTRE".equals(acesso.role());
    }
}
