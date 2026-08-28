package com.arcana.backend;

import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.user.dto.request.LoginRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.LoginResponseDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.arcana.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepositorie repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UsuarioService service;

    @Test
    void deveCadastrarUsuario() {
        UsuarioRequestDto dto = new UsuarioRequestDto("joao", "joao@gmail.com", "12334");
        Usuario newUser = new Usuario("joao", "joao@gmail.com", "encodedPassword");

        when(passwordEncoder.encode("12334")).thenReturn("encodedPassword");
        when(repository.save(any(Usuario.class))).thenReturn(newUser);

        UsuarioResponseDto dtoReturn = service.criar(dto);

        assertNotNull(dtoReturn);
        assertEquals("joao@gmail.com", dtoReturn.email());

        verify(passwordEncoder).encode("12334");
        verify(repository).save(any(Usuario.class));
    }

    @Test
    void deveLancarExcecaoQuandoUsernameJaExistir() {
        UsuarioRequestDto dto = new UsuarioRequestDto("theron", "joao@gmail.com", "12334");
        Usuario existsUser = new Usuario("theron", "joao@gmail.com", "12334");

        when(repository.findByUsername(dto.username())).thenReturn(Optional.of(existsUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.criar(dto);
        });

        assertEquals("Username já está em uso: " + dto.username(), exception.getMessage());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void deveAutenticarELogarUsuarioComTokenJwt() {
        LoginRequestDto loginDto = new LoginRequestDto("joao", "12334");
        Usuario user = new Usuario("joao", "joao@gmail.com", "encodedPassword");
        user.setId(1L);

        when(repository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("12334", "encodedPassword")).thenReturn(true);
        when(tokenService.gerarToken(user)).thenReturn("mocked-jwt-token");

        LoginResponseDto loginResponse = service.login(loginDto);

        assertNotNull(loginResponse);
        assertEquals("joao", loginResponse.username());
        assertEquals("mocked-jwt-token", loginResponse.token());
        assertEquals(1L, loginResponse.id());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaInvalidaNoLogin() {
        LoginRequestDto loginDto = new LoginRequestDto("joao", "senha_errada");
        Usuario user = new Usuario("joao", "joao@gmail.com", "encodedPassword");

        when(repository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha_errada", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.login(loginDto);
        });

        assertEquals("Usuário ou senha inválidos.", exception.getMessage());
    }
}

