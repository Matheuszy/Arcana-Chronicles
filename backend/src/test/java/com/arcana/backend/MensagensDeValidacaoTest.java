package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
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
}
