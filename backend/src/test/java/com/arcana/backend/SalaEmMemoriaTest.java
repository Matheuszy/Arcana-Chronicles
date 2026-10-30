package com.arcana.backend;

import com.arcana.backend.chat.sala.ItemDto;
import com.arcana.backend.chat.sala.ItensDaMesa;
import com.arcana.backend.chat.sala.NotasDaMesa;
import com.arcana.backend.chat.sala.StatusDaMesa;
import com.arcana.backend.chat.sala.StatusDto;
import com.arcana.backend.table.AlvoDeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SalaEmMemoriaTest {

    private static final Instant AGORA = Instant.parse("2026-10-30T12:00:00Z");
    private static final AlvoDeStatus JOGADOR = new AlvoDeStatus(100L, "Thorn", false, 20L);
    private static final AlvoDeStatus NPC = new AlvoDeStatus(200L, "Goblin", true, 10L);

    @Test
    void deveGuardarNotaPorUsuarioSemMisturarAsPessoas() {
        NotasDaMesa notas = new NotasDaMesa();

        notas.salvar(1L, "segredo da Aria");
        notas.salvar(2L, "segredo do Brom");

        assertEquals("segredo da Aria", notas.de(1L));
        assertEquals("segredo do Brom", notas.de(2L));
        assertEquals("", notas.de(3L));
    }

    @Test
    void deveApagarANotaQuandoOTextoFicaEmBranco() {
        NotasDaMesa notas = new NotasDaMesa();
        notas.salvar(1L, "algo");

        assertEquals("", notas.salvar(1L, "   "));
        assertEquals("", notas.de(1L));
    }

    @Test
    void deveRecusarNotaAcimaDoLimite() {
        NotasDaMesa notas = new NotasDaMesa();

        assertThrows(IllegalArgumentException.class,
                () -> notas.salvar(1L, "x".repeat(NotasDaMesa.LIMITE_CARACTERES + 1)));
        assertDoesNotThrow(() -> notas.salvar(1L, "x".repeat(NotasDaMesa.LIMITE_CARACTERES)));
    }

    @Test
    void deveSubstituirOStatusDaFichaEManterHistorico() {
        StatusDaMesa status = new StatusDaMesa();

        status.definir(JOGADOR, "Envenenado", AGORA);
        status.definir(JOGADOR, "Caído", AGORA);

        StatusDto visao = status.visaoPara(true);
        assertEquals(1, visao.atuais().size());
        assertEquals("Caído", visao.atuais().get(0).texto());
        assertEquals(2, visao.historico().size());
    }

    @Test
    void deveRegistrarRemocaoNoHistoricoEExigirStatusExistente() {
        StatusDaMesa status = new StatusDaMesa();
        status.definir(JOGADOR, "Envenenado", AGORA);

        StatusDto.Registro removido = status.remover(100L, AGORA);

        assertEquals(StatusDaMesa.REMOVIDO, removido.acao());
        assertTrue(status.visaoPara(true).atuais().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> status.remover(100L, AGORA));
    }

    @Test
    void deveEsconderStatusDeNpcDeQuemNaoEMestre() {
        StatusDaMesa status = new StatusDaMesa();
        status.definir(JOGADOR, "Envenenado", AGORA);
        status.definir(NPC, "Furioso", AGORA);

        StatusDto doMestre = status.visaoPara(true);
        StatusDto doJogador = status.visaoPara(false);

        assertEquals(2, doMestre.atuais().size());
        assertEquals(2, doMestre.historico().size());
        assertEquals(List.of("Thorn"), doJogador.atuais().stream().map(StatusDto.Atual::nome).toList());
        assertEquals(1, doJogador.historico().size());
    }

    @Test
    void deveValidarTextoDoStatus() {
        StatusDaMesa status = new StatusDaMesa();

        assertThrows(IllegalArgumentException.class, () -> status.definir(JOGADOR, "  ", AGORA));
        assertThrows(IllegalArgumentException.class, () -> status.definir(JOGADOR, null, AGORA));
        assertThrows(IllegalArgumentException.class,
                () -> status.definir(JOGADOR, "x".repeat(StatusDaMesa.LIMITE_CARACTERES + 1), AGORA));
    }

    @Test
    void deveLimitarFichasComStatusEOHistorico() {
        StatusDaMesa status = new StatusDaMesa();
        for (long id = 1; id <= StatusDaMesa.LIMITE_FICHAS; id++) {
            status.definir(new AlvoDeStatus(id, "Ficha " + id, false, id), "ok", AGORA);
        }

        assertThrows(IllegalStateException.class,
                () -> status.definir(new AlvoDeStatus(999L, "Extra", false, 999L), "ok", AGORA));
        assertDoesNotThrow(() -> status.definir(new AlvoDeStatus(1L, "Ficha 1", false, 1L), "outro", AGORA));

        for (int i = 0; i < StatusDaMesa.LIMITE_HISTORICO; i++) {
            status.definir(new AlvoDeStatus(1L, "Ficha 1", false, 1L), "volta " + i, AGORA);
        }
        assertEquals(StatusDaMesa.LIMITE_HISTORICO, status.visaoPara(true).historico().size());
    }

    @Test
    void deveRemoverOsStatusDasFichasDeUmJogadorQueSai() {
        StatusDaMesa status = new StatusDaMesa();
        status.definir(JOGADOR, "Envenenado", AGORA);
        status.definir(NPC, "Furioso", AGORA);

        List<StatusDto.Registro> removidos = status.removerDoDono(20L, AGORA);

        assertEquals(1, removidos.size());
        assertEquals("Thorn", removidos.get(0).nome());
        assertEquals(List.of("Goblin"), status.visaoPara(true).atuais().stream().map(StatusDto.Atual::nome).toList());
    }

    @Test
    void deveCriarItemComNomeObrigatorioEDescricaoOpcional() {
        ItensDaMesa itens = new ItensDaMesa();

        ItemDto item = itens.adicionar(20L, "Aria", "  Chave enferrujada ", null, AGORA);

        assertEquals("Chave enferrujada", item.nome());
        assertEquals("", item.descricao());
        assertEquals("20", item.autorId());
        assertThrows(IllegalArgumentException.class, () -> itens.adicionar(20L, "Aria", "  ", "desc", AGORA));
    }

    @Test
    void deveRespeitarLimitesDeNomeDescricaoEQuantidadeDeItens() {
        ItensDaMesa itens = new ItensDaMesa();

        assertThrows(IllegalArgumentException.class,
                () -> itens.adicionar(1L, "A", "x".repeat(ItensDaMesa.LIMITE_NOME + 1), "", AGORA));
        assertThrows(IllegalArgumentException.class,
                () -> itens.adicionar(1L, "A", "ok", "x".repeat(ItensDaMesa.LIMITE_DESCRICAO + 1), AGORA));
        assertDoesNotThrow(() -> itens.adicionar(1L, "A", "x".repeat(ItensDaMesa.LIMITE_NOME),
                "x".repeat(ItensDaMesa.LIMITE_DESCRICAO), AGORA));

        for (int i = 1; i < ItensDaMesa.LIMITE_ITENS; i++) {
            itens.adicionar(1L, "A", "item " + i, "", AGORA);
        }
        assertThrows(IllegalStateException.class, () -> itens.adicionar(1L, "A", "um a mais", "", AGORA));
    }

    @Test
    void devePermitirRemoverItemSoAoAutorOuAoMestre() {
        ItensDaMesa itens = new ItensDaMesa();
        ItemDto doAria = itens.adicionar(20L, "Aria", "Adaga", "", AGORA);
        ItemDto outro = itens.adicionar(20L, "Aria", "Mapa", "", AGORA);

        assertThrows(AccessDeniedException.class, () -> itens.remover(doAria.id(), 30L, false));
        assertEquals(2, itens.lista().size());

        itens.remover(doAria.id(), 20L, false);
        itens.remover(outro.id(), 10L, true);

        assertTrue(itens.lista().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> itens.remover(outro.id(), 10L, true));
    }
}
