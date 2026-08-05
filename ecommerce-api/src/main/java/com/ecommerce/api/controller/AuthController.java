package com.ecommerce.api.controller;

import com.ecommerce.api.dto.LoginRequest;
import com.ecommerce.api.dto.LoginResponse;
import com.ecommerce.api.dto.UsuarioDTO;
import com.ecommerce.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro")
public class AuthController {
    
    private final UsuarioService usuarioService;
    
    @PostMapping("/registrar")
    @Operation(summary = "Registra novo usuário")
    public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody UsuarioDTO dto) {
        log.info("Registrando novo usuário: {}", dto.getEmail());
        UsuarioDTO usuario = usuarioService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
    
    @PostMapping("/login")
    @Operation(summary = "Realiza login e retorna token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login realizado para: {}", request.getEmail());
        LoginResponse response = usuarioService.login(request);
        return ResponseEntity.ok(response);
    }
}