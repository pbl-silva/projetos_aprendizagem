package com.ecommerce.api.service;

import com.ecommerce.api.dto.LoginRequest;
import com.ecommerce.api.dto.LoginResponse;
import com.ecommerce.api.dto.UsuarioDTO;
import com.ecommerce.api.exception.RecursoNaoEncontradoException;
import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.model.Cliente;
import com.ecommerce.api.repository.ClienteRepository;
import com.ecommerce.api.repository.UsuarioRepository;
import com.ecommerce.api.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioService {
    
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
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
            // O endpoint de registro e publico. Aceitar o papel enviado pelo
            // cliente permitiria que qualquer pessoa se tornasse ADMIN.
            .papel(Usuario.Papel.USER)
            .ativo(true)
            .build();
        
        Usuario salvo = usuarioRepository.save(usuario);

        // Toda conta publica representa um cliente. O vinculo por e-mail
        // permite criar pedidos sem aceitar um clienteId manipulavel no front.
        if (clienteRepository.findByEmail(salvo.getEmail()).isEmpty()) {
            clienteRepository.save(Cliente.builder()
                .nome(salvo.getNome())
                .email(salvo.getEmail())
                .ativo(true)
                .build());
        }
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

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            log.warn("Tentativa de login de usuário inativo: {}", request.getEmail());
            throw new IllegalArgumentException("Usuário inativo");
        }
        
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            log.warn("Tentativa de login com senha incorreta: {}", request.getEmail());
            throw new IllegalArgumentException("Senha incorreta");
        }
        
        String token = jwtProvider.generateToken(
            usuario.getEmail(), usuario.getId(), usuario.getPapel());
        var clienteEncontrado = clienteRepository.findByEmail(usuario.getEmail());
        Long clienteId = clienteEncontrado.isPresent() ? clienteEncontrado.get().getId() : null;
        log.info("Login realizado com sucesso: {}", usuario.getEmail());
        
        return LoginResponse.of(token, usuario.getId(), usuario.getEmail(), usuario.getNome(),
            usuario.getPapel(), clienteId);
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
