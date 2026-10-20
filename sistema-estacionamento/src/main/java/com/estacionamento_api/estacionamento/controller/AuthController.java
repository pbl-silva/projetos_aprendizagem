package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.LoginRequestDTO;
import com.estacionamento_api.estacionamento.dto.LoginResponseDTO;
import com.estacionamento_api.estacionamento.dto.RegistroDTO;
import com.estacionamento_api.estacionamento.dto.UsuarioDTO;
import com.estacionamento_api.estacionamento.dto.AtualizacaoContaDTO;
import com.estacionamento_api.estacionamento.dto.AlteracaoSenhaDTO;
import com.estacionamento_api.estacionamento.dto.SessaoContaDTO;
import org.springframework.security.core.Authentication;
import com.estacionamento_api.estacionamento.security.JwtService;
import com.estacionamento_api.estacionamento.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/registrar")
    @Operation(summary = "Criar conta de administrador ou funcionário (somente administrador)")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroDTO dto) {
        usuarioService.registrar(dto.getUsername(), dto.getSenha(), dto.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensagem", "Usuário registrado com sucesso"));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> usuarioAtual(Authentication authentication) {
        return ResponseEntity.ok(usuarioService.obterUsuario(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<SessaoContaDTO> atualizarConta(Authentication authentication,
            @Valid @RequestBody AtualizacaoContaDTO dto) {
        UsuarioDTO usuario = usuarioService.atualizarConta(authentication.getName(), dto);
        return ResponseEntity.ok(new SessaoContaDTO(usuario, jwtService.gerarToken(usuario.username())));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<SessaoContaDTO> alterarSenha(Authentication authentication,
            @Valid @RequestBody AlteracaoSenhaDTO dto) {
        UsuarioDTO usuario = usuarioService.alterarSenha(authentication.getName(), dto);
        return ResponseEntity.ok(new SessaoContaDTO(usuario, jwtService.gerarToken(usuario.username())));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter um token JWT")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        UserDetails usuario;
        try {
            usuario = usuarioService.loadUserByUsername(dto.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getPassword())) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }

        String token = jwtService.gerarToken(usuario.getUsername());

        return ResponseEntity.ok(LoginResponseDTO.builder()
            .token(token)
            .tipo("Bearer")
            .username(usuario.getUsername())
            .expiraEmSegundos(jwtService.getExpiracaoSegundos())
            .build());
    }
}
