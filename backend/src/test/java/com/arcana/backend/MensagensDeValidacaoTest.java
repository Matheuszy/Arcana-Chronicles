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
    void deveExigirOsCamposObrigatoriosDaFichaERecusarVidaTemporariaNegativa() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(), null, null, null, null, " ", null, null, null, -1);

        assertEquals(List.of("A ficha precisa de ao menos uma classe", "Alinhamento é obrigatório",
                "Bônus de proficiência é obrigatório", "Raça é obrigatória",
                "Vida temporária não pode ser negativa"), mensagens(validador.validate(ficha)));
    }

    @Test
    void deveAceitarFichaSemOsCamposOpcionais() {
        var ficha = new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null, null,
                List.of(new PersonagemRequestDto.ClasseDto("Mago", null, 1)), 2, null, null, null,
                "Elfo", "Neutro e Bom", null, null, null);

        assertEquals(List.of(), mensagens(validador.validate(ficha)));
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
