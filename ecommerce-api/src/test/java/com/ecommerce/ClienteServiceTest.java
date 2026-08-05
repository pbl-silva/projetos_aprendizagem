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
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.service.ClienteService;

@ExtendWith(MockitoExtension.class)
public class ClienteServiceTest {
    
    @Mock
    private ClienteRepository clienteRepository;
    
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
}