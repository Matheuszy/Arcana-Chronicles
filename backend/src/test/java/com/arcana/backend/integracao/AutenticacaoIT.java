package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacaoIT extends IntegracaoTest {

    @Test
    void deveCadastrarEEntrarDevolvendoTokenEPapel() throws Exception {
        String username = nomeUnico("aria");
        postComo(null, "/api/users/register", Map.of(
                "username", username, "email", username + "@arcana.test",
                "password", "senha-secreta", "papel", "MESTRE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist());

        postComo(null, "/api/users/login", Map.of("username", username, "password", "senha-secreta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("MESTRE"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void deveRecusarLoginComSenhaErrada() throws Exception {
        Conta conta = registrar("JOGADOR");

        postComo(null, "/api/users/login", Map.of("username", conta.username(), "password", "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuário ou senha inválidos."));
    }

    @Test
    void deveRecusarCadastroComUsernameRepetido() throws Exception {
        Conta conta = registrar("JOGADOR");

        postComo(null, "/api/users/register", Map.of(
                "username", conta.username(), "email", "outro@arcana.test",
                "password", "senha-secreta", "papel", "JOGADOR"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRecusarCadastroInvalidoListandoMensagensEmPortugues() throws Exception {
        postComo(null, "/api/users/register", Map.of(
                "username", "", "email", "nao-e-email", "password", "123", "papel", "JOGADOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos."))
                .andExpect(jsonPath("$.details", hasItem("Email inválido")))
                .andExpect(jsonPath("$.details", hasItem("Senha deve ter entre 6 e 72 caracteres")));
    }

    @Test
    void deveBloquearRotaProtegidaSemToken() throws Exception {
        getComo(null, "/api/tables").andExpect(status().isUnauthorized());
        getComo(null, "/api/characters").andExpect(status().isUnauthorized());
    }

    @Test
    void deveBloquearRotaProtegidaComTokenInvalido() throws Exception {
        getComo("token.invalido.qualquer", "/api/tables")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Sessão expirada ou token inválido. Entre novamente."));
    }

    @Test
    void devePermitirRotaProtegidaComTokenValido() throws Exception {
        Conta conta = registrar("JOGADOR");

        getComo(conta.token(), "/api/tables").andExpect(status().isOk());
    }

    @Test
    void deveImpedirUsuarioDeVerOuAlterarOutroUsuario() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");

        getComo(aria.token(), "/api/users/" + aria.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(aria.username()));
        getComo(aria.token(), "/api/users/" + brom.id()).andExpect(status().isForbidden());
        deleteComo(aria.token(), "/api/users/" + brom.id()).andExpect(status().isForbidden());
        getComo(brom.token(), "/api/users/" + brom.id()).andExpect(status().isOk());
    }

    @Test
    void naoDeveExporSenhaNoPerfil() throws Exception {
        Conta conta = registrar("MESTRE");

        getComo(conta.token(), "/api/users/" + conta.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(not(containsString("senha-secreta"))));
    }

    @Test
    void deveEditarNomeEEmailSemSenhaEMantendoOLogin() throws Exception {
        Conta conta = registrar("JOGADOR");
        String novoNome = nomeUnico("renomeado");

        putComo(conta.token(), "/api/users/" + conta.id(), Map.of(
                "username", novoNome, "email", novoNome + "@arcana.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(novoNome))
                .andExpect(jsonPath("$.papel").value("JOGADOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        postComo(null, "/api/users/login", Map.of("username", novoNome, "password", "senha-secreta"))
                .andExpect(status().isOk());
    }

    @Test
    void deveTrocarASenhaSemExigirASenhaAtual() throws Exception {
        Conta conta = registrar("JOGADOR");

        putComo(conta.token(), "/api/users/" + conta.id(), Map.of(
                "username", conta.username(), "email", conta.username() + "@arcana.test", "password", "outra-senha"))
                .andExpect(status().isOk());

        postComo(null, "/api/users/login", Map.of("username", conta.username(), "password", "outra-senha"))
                .andExpect(status().isOk());
        postComo(null, "/api/users/login", Map.of("username", conta.username(), "password", "senha-secreta"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarEdicaoComNomeOuEmailDeOutraContaEComSenhaCurta() throws Exception {
        Conta aria = registrar("JOGADOR");
        Conta brom = registrar("JOGADOR");

        putComo(aria.token(), "/api/users/" + aria.id(), Map.of(
                "username", brom.username(), "email", aria.username() + "@arcana.test"))
                .andExpect(status().isConflict());
        putComo(aria.token(), "/api/users/" + aria.id(), Map.of(
                "username", aria.username(), "email", brom.username() + "@arcana.test"))
                .andExpect(status().isConflict());
        putComo(aria.token(), "/api/users/" + aria.id(), Map.of(
                "username", aria.username(), "email", aria.username() + "@arcana.test", "password", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("Senha deve ter entre 6 e 72 caracteres")));
    }

    @Test
    void deveRecusarCadastroComEmailJaUsado() throws Exception {
        Conta existente = registrar("JOGADOR");

        postComo(null, "/api/users/register", Map.of(
                "username", nomeUnico("outro"),
                "email", existente.username() + "@arcana.test",
                "password", "senha-secreta",
                "papel", "JOGADOR"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveExcluirContaLimpandMesasEParticipacoes() throws Exception {
        Conta mestre = registrar("MESTRE");
        Long mesaId = criarMesa(mestre);
        JogadorNaMesa jogador = jogadorAceitoNaMesa(mestre, mesaId);
        Conta outroMestre = registrar("MESTRE");
        Long mesaDeOutro = criarMesa(outroMestre);

        deleteComo(jogador.conta().token(), "/api/users/" + jogador.conta().id()).andExpect(status().isNoContent());
        assertFalse(corpo(getComo(mestre.token(), "/api/tables/" + mesaId)).get("participants").toString()
                .contains(jogador.conta().username()));

        deleteComo(mestre.token(), "/api/users/" + mestre.id()).andExpect(status().isNoContent());
        getComo(outroMestre.token(), "/api/tables/" + mesaId).andExpect(status().isNotFound());
        getComo(outroMestre.token(), "/api/tables/" + mesaDeOutro).andExpect(status().isOk());
        getComo(mestre.token(), "/api/tables").andExpect(status().isUnauthorized());
    }

    @Test
    void deveResponderConflitoEmCadastrosSimultaneosDoMesmoUsername() throws Exception {
        String username = nomeUnico("corrida");
        int tentativas = 6;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(tentativas);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            int indice = i;
            Callable<Integer> cadastro = () -> {
                largada.await();
                return postComo(null, "/api/users/register", Map.of(
                        "username", username, "email", "corrida" + indice + "_" + username + "@arcana.test",
                        "password", "senha-secreta", "papel", "JOGADOR"))
                        .andReturn().getResponse().getStatus();
            };
            resultados.add(executor.submit(cadastro));
        }
        largada.countDown();

        int criados = 0;
        for (Future<Integer> resultado : resultados) {
            int status = resultado.get();
            if (status == 201) {
                criados++;
            } else {
                assertEquals(409, status);
            }
        }
        executor.shutdown();
        assertEquals(1, criados);
    }

    @Test
    void deveRecusarSenhaMaiorQueOLimiteDoBcrypt() throws Exception {
        postComo(null, "/api/users/register", Map.of(
                "username", nomeUnico("longa"), "email", nomeUnico("longa") + "@arcana.test",
                "password", "x".repeat(73), "papel", "JOGADOR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem("Senha deve ter entre 6 e 72 caracteres")));
    }
}
