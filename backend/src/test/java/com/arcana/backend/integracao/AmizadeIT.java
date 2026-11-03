package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AmizadeIT extends IntegracaoTest {

    private Long enviarPedido(Conta remetente, Conta destinatario) throws Exception {
        return corpo(postComo(remetente.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", destinatario.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))).get("id").asLong();
    }

    private String codigoDe(Conta conta) throws Exception {
        return corpo(getComo(conta.token(), "/api/users/" + conta.id())).get("codigoAmigo").asText();
    }

    @Test
    void deveExigirAutenticacaoNasRotasDeAmizade() throws Exception {
        getComo(null, "/api/amigos/busca?termo=aria").andExpect(status().isUnauthorized());
        getComo(null, "/api/amigos/solicitacoes/recebidas").andExpect(status().isUnauthorized());
        postComo(null, "/api/amigos/solicitacoes", Map.of("destinatarioId", 1)).andExpect(status().isUnauthorized());
    }

    @Test
    void deveBuscarPorTrechoDoNomeEPeloCodigoSemIncluirOProprioUsuario() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("MESTRE");
        String codigo = codigoDe(brom).toLowerCase();

        getComo(aria.token(), "/api/amigos/busca?termo=" + brom.username().substring(3).toUpperCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + brom.id() + ")].relacao").value("NENHUMA"))
                .andExpect(jsonPath("$[?(@.id == " + aria.id() + ")]").isEmpty());
        getComo(aria.token(), "/api/amigos/busca?termo=" + codigo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(brom.id()))
                .andExpect(jsonPath("$[0].codigoAmigo").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
        getComo(aria.token(), "/api/amigos/busca?termo=" + codigoDe(aria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        getComo(aria.token(), "/api/amigos/busca?termo=ab").andExpect(status().isBadRequest());
    }

    @Test
    void deveEnviarEAceitarPedidoApenasPeloDestinatario() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        Conta intruso = registrar("JOGADOR");
        Long pedidoId = enviarPedido(aria, brom);

        getComo(brom.token(), "/api/amigos/solicitacoes/recebidas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pedidoId))
                .andExpect(jsonPath("$[0].remetenteUsername").value(aria.username()));
        getComo(aria.token(), "/api/amigos/solicitacoes/enviadas")
                .andExpect(jsonPath("$[0].id").value(pedidoId));

        postComo(aria.token(), "/api/amigos/solicitacoes/" + pedidoId + "/aceitar", null)
                .andExpect(status().isForbidden());
        postComo(intruso.token(), "/api/amigos/solicitacoes/" + pedidoId + "/aceitar", null)
                .andExpect(status().isForbidden());
        postComo(brom.token(), "/api/amigos/solicitacoes/" + pedidoId + "/aceitar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"));

        getComo(brom.token(), "/api/amigos/solicitacoes/recebidas").andExpect(jsonPath("$", hasSize(0)));
        getComo(aria.token(), "/api/amigos/busca?termo=" + brom.username())
                .andExpect(jsonPath("$[0].relacao").value("AMIGOS"));
        postComo(aria.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", brom.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Vocês já são amigos."));
    }

    @Test
    void deveRecusarPedidoApenasPeloDestinatarioEExigirEsperaParaReenviar() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("MESTRE");
        Long pedidoId = enviarPedido(aria, brom);

        postComo(aria.token(), "/api/amigos/solicitacoes/" + pedidoId + "/recusar", null)
                .andExpect(status().isForbidden());
        postComo(brom.token(), "/api/amigos/solicitacoes/" + pedidoId + "/recusar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"));

        postComo(aria.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", brom.id()))
                .andExpect(status().isConflict());
        enviarPedido(brom, aria);
    }

    @Test
    void deveCancelarPedidoApenasPeloRemetente() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        Long pedidoId = enviarPedido(aria, brom);

        postComo(brom.token(), "/api/amigos/solicitacoes/" + pedidoId + "/cancelar", null)
                .andExpect(status().isForbidden());
        postComo(aria.token(), "/api/amigos/solicitacoes/" + pedidoId + "/cancelar", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        postComo(brom.token(), "/api/amigos/solicitacoes/" + pedidoId + "/aceitar", null)
                .andExpect(status().isConflict());

        enviarPedido(aria, brom);
    }

    @Test
    void deveImpedirAutoamizadeEPedidoDuplicado() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");

        postComo(aria.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", aria.id()))
                .andExpect(status().isConflict());
        enviarPedido(aria, brom);
        postComo(aria.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", brom.id()))
                .andExpect(status().isConflict());
    }

    @Test
    void deveAceitarAutomaticamenteQuandoOsDoisSePedemEmAmizade() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("MESTRE");
        Long pedidoId = enviarPedido(aria, brom);

        postComo(brom.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", aria.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(pedidoId))
                .andExpect(jsonPath("$.status").value("ACEITA"));
    }

    @Test
    void deveManterUmaUnicaSolicitacaoAtivaEmPedidosCruzadosSimultaneos() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        int rodadas = 4;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(rodadas * 2);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < rodadas; i++) {
            resultados.add(executor.submit(pedidoSimultaneo(largada, aria, brom)));
            resultados.add(executor.submit(pedidoSimultaneo(largada, brom, aria)));
        }
        largada.countDown();
        for (Future<Integer> resultado : resultados) {
            int status = resultado.get();
            assertTrue(status == 201 || status == 409, "status inesperado: " + status);
        }
        executor.shutdown();

        int pendentes = corpo(getComo(aria.token(), "/api/amigos/solicitacoes/enviadas")).size()
                + corpo(getComo(aria.token(), "/api/amigos/solicitacoes/recebidas")).size();
        String relacao = corpo(getComo(aria.token(), "/api/amigos/busca?termo=" + brom.username()))
                .get(0).get("relacao").asText();
        assertTrue(pendentes == 1 || (pendentes == 0 && relacao.equals("AMIGOS")),
                "pendentes=" + pendentes + " relacao=" + relacao);
    }

    @Test
    void deveListarERemoverAmigosApenasDoProprioUsuario() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("MESTRE");
        Conta cael = registrar("JOGADOR");
        tornarAmigos(aria, brom);

        getComo(aria.token(), "/api/amigos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuarioId").value(brom.id()))
                .andExpect(jsonPath("$[0].papel").value("MESTRE"))
                .andExpect(jsonPath("$[0].presenca").value("OFFLINE"));
        getComo(cael.token(), "/api/amigos").andExpect(jsonPath("$", hasSize(0)));

        deleteComo(cael.token(), "/api/amigos/" + brom.id()).andExpect(status().isNotFound());
        deleteComo(brom.token(), "/api/amigos/" + aria.id()).andExpect(status().isNoContent());

        getComo(aria.token(), "/api/amigos").andExpect(jsonPath("$", hasSize(0)));
        enviarPedido(aria, brom);
    }

    @Test
    void deveBloquearDesfazendoAmizadeOcultandoDaBuscaEImpedindoPedidosNosDoisSentidos() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        tornarAmigos(aria, brom);

        postComo(aria.token(), "/api/amigos/bloqueios", Map.of("usuarioId", brom.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(brom.id()));

        getComo(aria.token(), "/api/amigos").andExpect(jsonPath("$", hasSize(0)));
        getComo(brom.token(), "/api/amigos").andExpect(jsonPath("$", hasSize(0)));
        getComo(aria.token(), "/api/amigos/bloqueios").andExpect(jsonPath("$[0].username").value(brom.username()));
        getComo(brom.token(), "/api/amigos/bloqueios").andExpect(jsonPath("$", hasSize(0)));

        getComo(brom.token(), "/api/amigos/busca?termo=" + aria.username()).andExpect(jsonPath("$", hasSize(0)));
        getComo(brom.token(), "/api/amigos/busca?termo=" + codigoDe(aria)).andExpect(jsonPath("$", hasSize(0)));
        getComo(aria.token(), "/api/amigos/busca?termo=" + brom.username()).andExpect(jsonPath("$", hasSize(0)));

        postComo(brom.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", aria.id()))
                .andExpect(status().isNotFound());
        postComo(aria.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", brom.id()))
                .andExpect(status().isConflict());
        postComo(aria.token(), "/api/amigos/bloqueios", Map.of("usuarioId", brom.id()))
                .andExpect(status().isConflict());

        deleteComo(brom.token(), "/api/amigos/bloqueios/" + aria.id()).andExpect(status().isNotFound());
        deleteComo(aria.token(), "/api/amigos/bloqueios/" + brom.id()).andExpect(status().isNoContent());
        enviarPedido(brom, aria);
    }

    @Test
    void deveCancelarPedidoPendenteAoBloquear() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");
        enviarPedido(brom, aria);

        postComo(aria.token(), "/api/amigos/bloqueios", Map.of("usuarioId", brom.id())).andExpect(status().isCreated());

        getComo(aria.token(), "/api/amigos/solicitacoes/recebidas").andExpect(jsonPath("$", hasSize(0)));
        getComo(brom.token(), "/api/amigos/solicitacoes/enviadas").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveExigirAutenticacaoParaListarAmigosEBloquear() throws Exception {
        getComo(null, "/api/amigos").andExpect(status().isUnauthorized());
        getComo(null, "/api/amigos/bloqueios").andExpect(status().isUnauthorized());
        postComo(null, "/api/amigos/bloqueios", Map.of("usuarioId", 1)).andExpect(status().isUnauthorized());
    }

    private Callable<Integer> pedidoSimultaneo(CountDownLatch largada, Conta remetente, Conta destinatario) {
        return () -> {
            largada.await();
            return postComo(remetente.token(), "/api/amigos/solicitacoes", Map.of("destinatarioId", destinatario.id()))
                    .andReturn().getResponse().getStatus();
        };
    }
}
