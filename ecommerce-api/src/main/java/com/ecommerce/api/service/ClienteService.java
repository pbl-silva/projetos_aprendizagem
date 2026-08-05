package com.ecommerce.api.service;

import com.ecommerce.api.dto.ClienteDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    
    public ClienteDTO criar(ClienteDTO dto) {
        Cliente cliente = Cliente.builder()
            .nome(dto.getNome())
            .cpf(dto.getCpf())
            .email(dto.getEmail())
            .telefone(dto.getTelefone())
            .endereco(dto.getEndereco())
            .ativo(true)
            .build();
        
        Cliente salvo = clienteRepository.save(cliente);
        return converterParaDTO(salvo);
    }
    
    @Transactional(readOnly = true)
    public ClienteDTO obterPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado com ID: " + id
            ));
        return converterParaDTO(cliente);
    }
    
    @Transactional(readOnly = true)
    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ClienteDTO> listarComPaginacao(int pagina, int tamanho, String ordenarPor, String direcao) {
        if (tamanho > 100) {
            tamanho = 100;
        }
        Sort.Direction direction = Sort.Direction.fromString(direcao.toUpperCase());
        Sort sort = Sort.by(direction, ordenarPor);
        Pageable pageable = PageRequest.of(pagina, tamanho, sort);
        return clienteRepository.findAll(pageable).map(this::converterParaDTO);
    }
    
    public ClienteDTO atualizar(Long id, ClienteDTO dto) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente não encontrado com ID: " + id
            ));
        
        cliente.setNome(dto.getNome());
        cliente.setCpf(dto.getCpf());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setEndereco(dto.getEndereco());
        
        Cliente atualizado = clienteRepository.save(cliente);
        return converterParaDTO(atualizado);
    }
    
    public void deletar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Cliente não encontrado com ID: " + id
            );
        }
        clienteRepository.deleteById(id);
    }
    
    private ClienteDTO converterParaDTO(Cliente cliente) {
        return ClienteDTO.builder()
            .id(cliente.getId())
            .nome(cliente.getNome())
            .email(cliente.getEmail())
            .cpf(cliente.getCpf())
            .telefone(cliente.getTelefone())
            .endereco(cliente.getEndereco())
            .ativo(cliente.getAtivo())
            .build();
    }
}