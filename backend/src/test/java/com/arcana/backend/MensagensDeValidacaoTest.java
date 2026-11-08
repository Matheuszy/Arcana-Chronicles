package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.model.TipoEquipamento;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.model.PapelUsuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MensagensDeValidacaoTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private List<String> mensagens(Set<? extends ConstraintViolation<?>> violacoes) {
        return violacoes.stream().map(ConstraintViolation::getMessage).sorted().toList();
    }

    @Test
    void deveExplicarEmPortuguesOsLimitesDosAtributos() {
        var atributos = new PersonagemRequestDto.AtributosDto(5, 21, null, 10, 10, 10);

        List<String> mensagens = mensagens(validador.validate(atributos));

        assertEquals(List.of("Constituição é obrigatória", "Destreza deve ser no máximo 20",
                "Força deve ser no mínimo 8"), mensagens);
    }

    @Test
    void deveExigirOsCamposObrigatoriosDaFichaComDeslocamentoERecusarVidaTemporariaNegativa() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(), null, null, null, null, " ", null, null, null, -1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(List.of("A ficha precisa de ao menos uma classe", "Alinhamento é obrigatório",
                "Bônus de proficiência é obrigatório", "Deslocamento é obrigatório", "Raça é obrigatória",
                "Vida temporária não pode ser negativa"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveAceitarFichaSemOsCamposOpcionais() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null, 9, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(List.of(), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveRecusarTestesContraAMorteForaDeZeroATres() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null, 9, true, 4, -1, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(List.of("Falhas nos testes contra a morte devem ser de 0 a 3",
                "Sucessos nos testes contra a morte devem ser de 0 a 3"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveRecusarIdiomaVazioEProficienciaLongaDemais() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null, 9, null, null, null,
                List.of(" "), List.of("x".repeat(61)), null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(List.of("Idioma não pode ser vazio",
                "Proficiência em ferramenta não pode ter mais de 60 caracteres"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveLimitarAntecedenteETracosDePersonalidade() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null, 9, null, null, null, null, null, null, null,
                "a".repeat(61), "t".repeat(1001), "i".repeat(1001), "v".repeat(1001), "d".repeat(1001), null, null, null, null, null, null);

        assertEquals(List.of("Antecedente não pode ter mais de 60 caracteres",
                "Defeitos não podem ter mais de 1000 caracteres", "Ideais não podem ter mais de 1000 caracteres",
                "Traços de personalidade não podem ter mais de 1000 caracteres",
                "Vínculos não podem ter mais de 1000 caracteres"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveExplicarEmPortuguesOsErrosDoDeslocamentoAdicional() {
        var deslocamento = new PersonagemRequestDto.DeslocamentoAdicionalDto(null, 301, "o".repeat(61), null);

        assertEquals(List.of("A origem do deslocamento não pode ter mais de 60 caracteres",
                "Escolha o tipo do deslocamento adicional",
                "O deslocamento adicional não pode passar de 300 metros"), mensagens(validador.validate(deslocamento)));
    }

    @Test
    void deveRecusarMoedasNegativasOuAcimaDoLimite() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null, 9, null, null, null, null, null, null, null,
                null, null, null, null, null, null, -1, null, null, 1000001, null);

        assertEquals(List.of("Moedas de cobre não podem ser negativas",
                "Moedas de ouro não podem passar de 1.000.000"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveExplicarEmPortuguesOsErrosDaArmaOuArmadura() {
        var equipamento = new PersonagemRequestDto.EquipamentoDto(null, " ", "1d7", null, 31, null);

        assertEquals(List.of("A CA da armadura não pode passar de 30",
                "Escolha se o equipamento é arma ou armadura", "O dado de dano deve ser como 1d8 ou 2d6",
                "Toda arma ou armadura precisa de um nome"), mensagens(validador.validate(equipamento)));
    }

    @Test
    void deveAceitarArmaComDadoDeDanoValido() {
        var arma = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Machado grande", "1d12", "forca", null, null);
        var espadas = new PersonagemRequestDto.EquipamentoDto(TipoEquipamento.ARMA, "Montante", "20d6", null, null, null);

        assertEquals(List.of(), mensagens(validador.validate(arma)));
        assertEquals(List.of(), mensagens(validador.validate(espadas)));
    }

    @Test
    void deveExplicarEmPortuguesOsErrosDaMagia() {
        var magia = new PersonagemRequestDto.SpellDto(" ", 12, null);

        assertEquals(List.of("O círculo da magia deve ser de 0 a 9", "Toda magia precisa de um nome"),
                mensagens(validador.validate(magia)));
    }

    @Test
    void deveExplicarEmPortuguesOsLimitesDeNomeDeMesaEUsername() {
        var mesa = new MesaRequestDto("x".repeat(256), null);
        var usuario = new UsuarioRequestDto("x".repeat(101), "a@b.com", "123456", PapelUsuario.JOGADOR);

        assertEquals(List.of("Nome da mesa não pode ter mais de 255 caracteres"), mensagens(validador.validate(mesa)));
        assertEquals(List.of("Username não pode ter mais de 100 caracteres"), mensagens(validador.validate(usuario)));
    }

    @Test
    void deveRecusarEmailSemDominioCompleto() {
        for (String email : List.of("a@b", "joao@gmail", "joao.gmail.com", "joao@.com", "joao@gmail.c", "jo ao@gmail.com")) {
            var usuario = new UsuarioRequestDto("joao", email, "123456", PapelUsuario.JOGADOR);

            assertEquals(List.of("Email inválido. Use o formato nome@dominio.com"),
                    mensagens(validador.validate(usuario)), email);
        }
    }

    @Test
    void deveAceitarEmailValidoNormalizandoEspacosEMaiusculas() {
        var usuario = new UsuarioRequestDto("  joao ", "  Joao.Silva+rpg@Mail.Arcana.com.br ", "123456", PapelUsuario.JOGADOR);

        assertEquals(List.of(), mensagens(validador.validate(usuario)));
        assertEquals("joao", usuario.username());
        assertEquals("joao.silva+rpg@mail.arcana.com.br", usuario.email());
    }
}
