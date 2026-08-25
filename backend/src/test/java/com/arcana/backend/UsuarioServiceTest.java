package com.arcana.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.arcana.backend.user.service.UsuarioService;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

		@Mock
		private UsuarioRepositorie repository;
		
		@InjectMocks
		private UsuarioService service;

		@Test
		void deveCadastrarUsuario(){

			UsuarioRequestDto dto = new UsuarioRequestDto("joao", "joao@gmail.com", "12334");
			Usuario newUser = new Usuario("joao", "joao@gmail.com", "12334");
			when(repository.save(any(Usuario.class))).thenReturn(newUser);

			UsuarioResponseDto dtoReturn = service.criar(dto);

			assertNotNull(dtoReturn);

			assertEquals("joao@gmail.com", dtoReturn.email());

			verify(repository).save(any(Usuario.class));

		}

		@Test
		void deveLancarExcecaoQuandoEmailJaExistir() {
		
			UsuarioRequestDto dto = new UsuarioRequestDto("theron", "joao@gmail.com", "12334");

			Usuario existsUser = new Usuario("theron", "joao@gmail.com", "12334");
			

			when(repository.findByUsername(dto.username())).thenReturn(Optional.of(existsUser));

			
			RuntimeException exception = assertThrows(RuntimeException.class, () -> {
				service.criar(dto);
			});

			assertEquals("Username já está em uso: " + dto.username(), exception.getMessage());

			verify(repository, never()).save(any(Usuario.class));
		}

}
