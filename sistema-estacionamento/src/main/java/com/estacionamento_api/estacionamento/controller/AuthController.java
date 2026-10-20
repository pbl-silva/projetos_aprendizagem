package com.estacionamento_api.estacionamento.controller;

import com.estacionamento_api.estacionamento.dto.LoginRequestDTO;
import com.estacionamento_api.estacionamento.dto.LoginResponseDTO;
import com.estacionamento_api.estacionamento.dto.RegistroDTO;
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
    @Operation(summary = "Registrar um novo usuário")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroDTO dto) {
        usuarioService.registrar(dto.getUsername(), dto.getSenha(), "OPERADOR");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("mensagem", "Usuário registrado com sucesso"));
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
