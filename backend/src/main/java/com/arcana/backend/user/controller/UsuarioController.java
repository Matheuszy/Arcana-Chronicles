package com.arcana.backend.user.controller;

import com.arcana.backend.user.dto.request.LoginRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.LoginResponseDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** POST /api/users/register — cadastro */
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDto> register(
            @RequestBody @Valid UsuarioRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.criar(dto));
    }

    /** POST /api/users/login — login */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody @Valid LoginRequestDto dto) {

        return ResponseEntity.ok(usuarioService.login(dto));
    }

    /** GET /api/users/{id} — perfil */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    /** PUT /api/users/{id} — atualizar dados */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioRequestDto dto) {

        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    /** DELETE /api/users/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
