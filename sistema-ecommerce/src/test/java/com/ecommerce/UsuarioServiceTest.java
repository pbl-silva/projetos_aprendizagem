package com.ecommerce;

import com.ecommerce.api.dto.LoginRequest;
import com.ecommerce.api.dto.LoginResponse;
import com.ecommerce.api.dto.UsuarioDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.UsuarioRepository;
import com.ecommerce.api.security.JwtProvider;
import com.ecommerce.api.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
            .id(1L)
            .email("usuario@teste.com")
            .senha("senha-criptografada")
            .nome("Usuário Teste")
            .papel(Usuario.Papel.USER)
            .ativo(true)
            .build();

        usuarioDTO = UsuarioDTO.builder()
            .email("usuario@teste.com")
            .senha("senha123")
            .nome("Usuário Teste")
            .papel(Usuario.Papel.USER)
            .ativo(true)
            .build();
    }

    @Test
    @DisplayName("Deve registrar novo usuário")
    void testRegistrar() {
        usuarioDTO.setPapel(Usuario.Papel.ADMIN);
        when(usuarioRepository.existsByEmail("usuario@teste.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha-criptografada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioDTO resultado = usuarioService.registrar(usuarioDTO);

        assertNotNull(resultado);
        assertEquals("usuario@teste.com", resultado.getEmail());
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals(Usuario.Papel.USER, usuarioCaptor.getValue().getPapel());
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar email já existente")
    void testRegistrarEmailExistente() {
        when(usuarioRepository.existsByEmail("usuario@teste.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrar(usuarioDTO));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve realizar login com sucesso")
    void testLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@teste.com");
        request.setSenha("senha123");

        when(usuarioRepository.findByEmail("usuario@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "senha-criptografada")).thenReturn(true);
        when(jwtProvider.generateToken("usuario@teste.com", 1L, Usuario.Papel.USER)).thenReturn("token-jwt-fake");

        LoginResponse resultado = usuarioService.login(request);

        assertNotNull(resultado);
        assertEquals("token-jwt-fake", resultado.getToken());
        assertEquals("usuario@teste.com", resultado.getEmail());
    }

    @Test
    @DisplayName("Deve lançar exceção ao logar com email inexistente")
    void testLoginEmailNaoEncontrado() {
        LoginRequest request = new LoginRequest();
        request.setEmail("naoexiste@teste.com");
        request.setSenha("senha123");

        when(usuarioRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> usuarioService.login(request));
    }

    @Test
    @DisplayName("Deve lançar exceção ao logar com senha incorreta")
    void testLoginSenhaIncorreta() {
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@teste.com");
        request.setSenha("senhaErrada");

        when(usuarioRepository.findByEmail("usuario@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "senha-criptografada")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.login(request));
    }

    @Test
    @DisplayName("Deve rejeitar login de usuario inativo")
    void testLoginUsuarioInativo() {
        LoginRequest request = new LoginRequest("usuario@teste.com", "senha123");
        usuario.setAtivo(false);
        when(usuarioRepository.findByEmail("usuario@teste.com")).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(jwtProvider);
    }

}
