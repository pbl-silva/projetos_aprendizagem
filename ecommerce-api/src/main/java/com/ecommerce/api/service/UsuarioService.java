package com.ecommerce.api.service;

import com.ecommerce.api.dto.LoginRequest;
import com.ecommerce.api.dto.LoginResponse;
import com.ecommerce.api.dto.UsuarioDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.repository.UsuarioRepository;
import com.ecommerce.api.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    
    /**
     * Registra novo usuário
     */
    public UsuarioDTO registrar(UsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            log.warn("Tentativa de registro com email já existente: {}", dto.getEmail());
            throw new IllegalArgumentException("Email já cadastrado");
        }
        
        Usuario usuario = Usuario.builder()
            .email(dto.getEmail())
            .senha(passwordEncoder.encode(dto.getSenha()))
            .nome(dto.getNome())
            .papel(dto.getPapel())
            .ativo(true)
            .build();
        
        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Novo usuário registrado: {}", salvo.getEmail());
        
        return converterParaDTO(salvo);
    }
    
    /**
     * Realiza login
     */
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Tentativa de login com email não encontrado: {}", request.getEmail());
                return new RecursoNaoEncontradoException("Usuário não encontrado");
            });
        
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            log.warn("Tentativa de login com senha incorreta: {}", request.getEmail());
            throw new IllegalArgumentException("Senha incorreta");
        }
        
        String token = jwtProvider.generateToken(usuario.getEmail(), usuario.getId());
        log.info("Login realizado com sucesso: {}", usuario.getEmail());
        
        return LoginResponse.of(token, usuario.getId(), usuario.getEmail(), usuario.getNome());
    }
    
    /**
     * Obtém usuário por ID
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obterPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Usuário não encontrado com ID: " + id
            ));
        return converterParaDTO(usuario);
    }
    
    /**
     * Lista todos os usuários
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll()
            .stream()
            .map(this::converterParaDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Atualiza usuário
     */
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Usuário não encontrado com ID: " + id
            ));
        
        usuario.setNome(dto.getNome());
        usuario.setAtivo(dto.getAtivo());
        
        Usuario atualizado = usuarioRepository.save(usuario);
        log.info("Usuário atualizado: {}", usuario.getEmail());
        
        return converterParaDTO(atualizado);
    }
    
    /**
     * Deleta usuário
     */
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                "Usuário não encontrado com ID: " + id
            );
        }
        usuarioRepository.deleteById(id);
        log.info("Usuário deletado com ID: {}", id);
    }
    
    private UsuarioDTO converterParaDTO(Usuario usuario) {
        return UsuarioDTO.builder()
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nome(usuario.getNome())
            .papel(usuario.getPapel())
            .ativo(usuario.getAtivo())
            .build();
    }
}