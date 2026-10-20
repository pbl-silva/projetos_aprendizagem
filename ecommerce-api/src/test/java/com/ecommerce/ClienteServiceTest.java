package com.ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.api.dto.ClienteDTO;
import com.ecommerce.api.dto.AtualizacaoClienteDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.UsuarioRepository;
import com.ecommerce.api.service.ClienteService;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {
    
    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @InjectMocks
    private ClienteService clienteService;
    
    private Cliente cliente;
    private ClienteDTO clienteDTO;
    
    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
            .id(1L)
            .nome("João Silva")
            .email("joao@example.com")
            .cpf("12345678901")
            .telefone("11987654321")
            .endereco("Rua A, 123")
            .ativo(true)
            // ✅ Remova a linha: .dataCriacao(LocalDateTime.now())
            .build();
        
        clienteDTO = ClienteDTO.builder()
            .id(1L)
            .nome("João Silva")
            .email("joao@example.com")
            .cpf("12345678901")
            .telefone("11987654321")
            .endereco("Rua A, 123")
            .ativo(true)
            .build();
    }
    
    @Test
    void testObterPorId() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        
        ClienteDTO resultado = clienteService.obterPorId(1L);
        
        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(clienteRepository, times(1)).findById(1L);
    }
    
    @Test
    void testObterPorIdNaoEncontrado() {
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(RecursoNaoEncontradoException.class, () -> {
            clienteService.obterPorId(999L);
        });
    }
    
    @Test
    void testCriarCliente() {
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);
        
        ClienteDTO resultado = clienteService.criar(clienteDTO);
        
        assertNotNull(resultado);
        assertEquals("João Silva", resultado.getNome());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
    }
    
    @Test
    void testDeletarCliente() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        
        clienteService.deletar(1L);
        
        verify(clienteRepository, times(1)).deleteById(1L);
    }

    @Test
    void testListarComPaginacao() {
        org.springframework.data.domain.Page<Cliente> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(cliente));
        when(clienteRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        var resultado = clienteService.listarComPaginacao(0, 10, "id", "ASC");

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void testObterMinhaContaPorEmail() {
        when(clienteRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(cliente));

        ClienteDTO resultado = clienteService.obterPorEmail("joao@example.com");

        assertEquals(1L, resultado.getId());
        assertEquals("João Silva", resultado.getNome());
    }

    @Test
    void testAtualizarMinhaContaESincronizarUsuario() {
        Usuario usuario = Usuario.builder()
            .id(7L)
            .nome("João Silva")
            .email("joao@example.com")
            .senha("hash")
            .build();
        AtualizacaoClienteDTO atualizacao = AtualizacaoClienteDTO.builder()
            .nome("João da Silva")
            .email("novo@example.com")
            .cpf("12345678901")
            .telefone("11999999999")
            .endereco("Rua Nova, 10")
            .build();

        when(clienteRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(clienteRepository.findByCpf("12345678901")).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        ClienteDTO resultado = clienteService.atualizarMinhaConta("joao@example.com", atualizacao);

        assertEquals("João da Silva", resultado.getNome());
        assertEquals("novo@example.com", resultado.getEmail());
        assertEquals("novo@example.com", usuario.getEmail());
        assertEquals("João da Silva", usuario.getNome());
        verify(usuarioRepository).save(usuario);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void testNaoPermitirEmailDeOutraConta() {
        Usuario usuario = Usuario.builder()
            .id(7L).nome("João Silva").email("joao@example.com").senha("hash").build();
        Cliente outroCliente = Cliente.builder()
            .id(2L).nome("Maria").email("maria@example.com").build();
        AtualizacaoClienteDTO atualizacao = AtualizacaoClienteDTO.builder()
            .nome("João Silva").email("maria@example.com").build();

        when(clienteRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(outroCliente));

        assertThrows(IllegalArgumentException.class,
            () -> clienteService.atualizarMinhaConta("joao@example.com", atualizacao));
        verify(clienteRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }
}
