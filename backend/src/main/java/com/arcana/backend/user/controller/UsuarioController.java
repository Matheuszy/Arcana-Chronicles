package com.arcana.backend.user.controller;

import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UsuarioController {

    private UsuarioService usuarioService;
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<UsuarioResponseDto> createUser(@RequestBody @Valid UsuarioRequestDto dto) {
        return ResponseEntity.ok(usuarioService.createUser(dto));
    }
}
