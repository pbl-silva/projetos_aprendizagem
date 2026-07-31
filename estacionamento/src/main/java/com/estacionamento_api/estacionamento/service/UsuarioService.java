package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.model.Usuario;
import com.estacionamento_api.estacionamento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuário não encontrado: " + username));

        return User.withUsername(usuario.getUsername())
            .password(usuario.getSenha())
            .authorities("ROLE_" + usuario.getRole())
            .build();
    }

    public Usuario registrar(String username, String senhaPura, String role) {
        if (usuarioRepository.existsByUsername(username)) {
            log.warn("Tentativa de registro com username já existente: {}", username);
            throw new IllegalArgumentException(
                "Já existe um usuário com o username: " + username);
        }

        Usuario usuario = Usuario.builder()
            .username(username)
            .senha(passwordEncoder.encode(senhaPura))
            .role(role)
            .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário registrado: {}", username);
        return salvo;
    }

    @Transactional(readOnly = true)
    public boolean existeUsuario(String username) {
        return usuarioRepository.existsByUsername(username);
    }
}
