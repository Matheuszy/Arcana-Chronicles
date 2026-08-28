package com.arcana.backend.character.controller;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.service.PersonagemService;
import com.arcana.backend.user.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class PersonagemController {

    private final PersonagemService personagemService;

    public PersonagemController(PersonagemService personagemService) {
        this.personagemService = personagemService;
    }

    /** GET /api/characters?kind=PLAYER */
    @GetMapping
    public ResponseEntity<List<PersonagemResponseDto>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) TipoJogador kind) {

        return ResponseEntity.ok(personagemService.listarPorUsuario(usuario.getId(), kind));
    }

    /** GET /api/characters/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.buscarPorId(id, usuario.getId()));
    }

    /** POST /api/characters */
    @PostMapping
    public ResponseEntity<PersonagemResponseDto> criar(
            @RequestBody @Valid PersonagemRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        PersonagemResponseDto criado = personagemService.criar(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/characters/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PersonagemRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.atualizar(id, dto, usuario.getId()));
    }

    /** DELETE /api/characters/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        personagemService.deletar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}

