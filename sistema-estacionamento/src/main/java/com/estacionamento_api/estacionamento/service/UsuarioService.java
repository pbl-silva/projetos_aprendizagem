package com.estacionamento_api.estacionamento.service;

import com.estacionamento_api.estacionamento.model.Usuario;
import com.estacionamento_api.estacionamento.repository.UsuarioRepository;
import com.estacionamento_api.estacionamento.dto.UsuarioDTO;
import com.estacionamento_api.estacionamento.dto.AtualizacaoContaDTO;
import com.estacionamento_api.estacionamento.dto.AlteracaoSenhaDTO;
import com.estacionamento_api.estacionamento.exception.RequisicaoInvalidaException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
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
        if (!"ADMIN".equals(role) && !"OPERADOR".equals(role)) {
            throw new IllegalArgumentException("Perfil de usuário inválido");
        }
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

    @Transactional(readOnly = true)
    public boolean existeAdministrador() {
        return usuarioRepository.existsByRole("ADMIN");
    }

    @Transactional(readOnly = true)
    public UsuarioDTO obterUsuario(String username) {
        return resumo(usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado")));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream().map(this::resumo).toList();
    }

    private UsuarioDTO resumo(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getRole(), usuario.getDataCriacao(),
            usuario.getNome(), usuario.getEmail(), usuario.getTelefone());
    }

    public UsuarioDTO atualizarConta(String usernameAtual, AtualizacaoContaDTO dto) {
        Usuario usuario = buscar(usernameAtual);
        confirmarSenha(usuario, dto.senhaAtual());
        String novoUsername = dto.username().trim();
        if (novoUsername.length() < 3) {
            throw new RequisicaoInvalidaException("O usuário deve ter pelo menos 3 caracteres");
        }
        if (!novoUsername.equals(usuario.getUsername()) && usuarioRepository.existsByUsername(novoUsername)) {
            throw new IllegalArgumentException("Este usuário já está em uso. Escolha outro.");
        }
        usuario.setUsername(novoUsername);
        usuario.setNome(limpar(dto.nome()));
        usuario.setEmail(limpar(dto.email()));
        usuario.setTelefone(limpar(dto.telefone()));
        return resumo(usuarioRepository.saveAndFlush(usuario));
    }

    public UsuarioDTO alterarSenha(String usernameAtual, AlteracaoSenhaDTO dto) {
        Usuario usuario = buscar(usernameAtual);
        confirmarSenha(usuario, dto.senhaAtual());
        if (!dto.novaSenha().equals(dto.confirmacaoSenha())) {
            throw new RequisicaoInvalidaException("A confirmação da nova senha não confere");
        }
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        return resumo(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional(readOnly = true)
    public String credencialAtual(String username) {
        Usuario usuario = buscar(username);
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(
                (usuario.getId() + ":" + usuario.getSenha()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Usuario buscar(String username) {
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    private void confirmarSenha(Usuario usuario, String senha) {
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new RequisicaoInvalidaException("A senha atual está incorreta");
        }
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
