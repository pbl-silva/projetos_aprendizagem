package com.biblioteca.biblioteca_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.biblioteca.biblioteca_api.dto.request.UsuarioRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.UsuarioResponseDTO;
import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.repositories.EmprestimoRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import com.biblioteca.biblioteca_api.services.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    @DisplayName("Deve criar usuário com email e CPF únicos")
    void deveCriarUsuarioComEmailECpfUnicos() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Maria",
                "maria@email.com",
                "12345678900",
                TipoUsuario.COMUM
        );

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(usuarioRepository.existsByCpf(request.getCpf())).thenReturn(false);
        when(usuarioRepository.save(ArgumentMatchers.any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setId(1L);
                    return u;
                });

        UsuarioResponseDTO resp = usuarioService.criar(request);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        verify(usuarioRepository).existsByEmail(request.getEmail());
        verify(usuarioRepository).existsByCpf(request.getCpf());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void deveLancarQuandoEmailExiste() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Maria",
                "maria@email.com",
                "12345678900",
                TipoUsuario.COMUM
        );

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.criar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já existe")
    void deveLancarQuandoCpfExiste() {
        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Maria",
                "maria@email.com",
                "12345678900",
                TipoUsuario.COMUM
        );

        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(usuarioRepository.existsByCpf(request.getCpf())).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.criar(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar usuário por id")
    void deveBuscarUsuarioPorId() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Paulo")
                .email("p@e.com")
                .cpf("11122233344")
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO resp = usuarioService.buscarPorId(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        assertEquals("Paulo", resp.getNome());
    }

    @Test
    @DisplayName("Deve lançar BusinessException (não IllegalStateException) ao deletar usuário com empréstimos ativos")
    void deveLancarBusinessExceptionAoDeletarComEmprestimosAtivos() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nome("Paulo")
                .email("p@e.com")
                .cpf("11122233344")
                .build();

        Emprestimo emprestimoAtivo = Emprestimo.builder()
                .id(5L)
                .usuario(usuario)
                .status(StatusEmprestimo.ATIVO)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(emprestimoRepository.findByUsuarioAndStatus(usuario, StatusEmprestimo.ATIVO))
                .thenReturn(List.of(emprestimoAtivo));

        assertThrows(BusinessException.class, () -> usuarioService.deletar(1L));
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve atualizar usuário sem conflito quando email/CPF não mudaram")
    void deveAtualizarUsuarioMantendoMesmoEmailECpf() {
        Usuario existente = Usuario.builder()
                .id(1L)
                .nome("Paulo")
                .email("p@e.com")
                .cpf("11122233344")
                .tipoUsuario(TipoUsuario.COMUM)
                .build();

        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Paulo Atualizado",
                "p@e.com",
                "11122233344",
                TipoUsuario.PREMIUM
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO resp = usuarioService.atualizar(1L, request);

        assertNotNull(resp);
        assertEquals("Paulo Atualizado", resp.getNome());
        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).existsByCpf(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar usuário para um email já usado por outro")
    void deveLancarQuandoAtualizarParaEmailJaExistente() {
        Usuario existente = Usuario.builder()
                .id(1L)
                .nome("Paulo")
                .email("p@e.com")
                .cpf("11122233344")
                .tipoUsuario(TipoUsuario.COMUM)
                .build();

        UsuarioRequestDTO request = new UsuarioRequestDTO(
                "Paulo",
                "outro@email.com",
                "11122233344",
                TipoUsuario.COMUM
        );

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmail("outro@email.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> usuarioService.atualizar(1L, request));
        verify(usuarioRepository, never()).save(any());
    }
}