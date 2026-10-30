package com.arcana.backend.integracao;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
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
                .andExpect(jsonPath("$.details", hasItem("Senha deve ter ao menos 6 caracteres")));
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
}
