package com.arcana.backend.character.controller;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.service.PersonagemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints consumidos pelo frontend em /api/characters
 *
 * Por ora o ownerId vem como header X-Owner-Id (temporário até ter Spring Security + JWT).
 * Quando o auth estiver pronto, trocar por: principal.getId() ou @AuthenticationPrincipal.
 */
@RestController
@RequestMapping("/api/characters")
@CrossOrigin(origins = "http://localhost:5173") // porta padrão do Vite
public class PersonagemController {

    private final PersonagemService personagemService;

    public PersonagemController(PersonagemService personagemService) {
        this.personagemService = personagemService;
    }

    /** GET /api/characters?kind=PLAYER */
    @GetMapping
    public ResponseEntity<List<PersonagemResponseDto>> listar(
            @RequestHeader("X-Owner-Id") Long ownerId,
            @RequestParam(required = false) TipoJogador kind) {

        return ResponseEntity.ok(personagemService.listarPorUsuario(ownerId, kind));
    }

    /** GET /api/characters/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> buscarPorId(
            @PathVariable Long id,
            @RequestHeader("X-Owner-Id") Long ownerId) {

        return ResponseEntity.ok(personagemService.buscarPorId(id, ownerId));
    }

    /** POST /api/characters */
    @PostMapping
    public ResponseEntity<PersonagemResponseDto> criar(
            @RequestBody @Valid PersonagemRequestDto dto,
            @RequestHeader("X-Owner-Id") Long ownerId) {

        PersonagemResponseDto criado = personagemService.criar(dto, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/characters/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PersonagemRequestDto dto,
            @RequestHeader("X-Owner-Id") Long ownerId) {

        return ResponseEntity.ok(personagemService.atualizar(id, dto, ownerId));
    }

    /** DELETE /api/characters/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @RequestHeader("X-Owner-Id") Long ownerId) {

        personagemService.deletar(id, ownerId);
        return ResponseEntity.noContent().build();
    }
}
