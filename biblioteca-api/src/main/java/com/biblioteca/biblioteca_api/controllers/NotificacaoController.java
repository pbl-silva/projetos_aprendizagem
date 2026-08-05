package com.biblioteca.biblioteca_api.controllers;

import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.exceptions.ResourceNotFoundException;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import com.biblioteca.biblioteca_api.services.NotificadorUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Endpoints para envio de notificações aos usuários")
public class NotificacaoController {

    // DIP: Depende de abstração (interface), não de implementação concreta
    private final NotificadorUsuario notificadorUsuario;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Enviar email de boas-vindas",
            description = "Envia um email de boas-vindas para o usuário especificado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email enviado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/usuario/{usuarioId}/boas-vindas")
    public ResponseEntity<String> enviarBoasVindas(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        notificadorUsuario.enviarEmailBoasVindas(usuario);
        return ResponseEntity.ok("Email de boas-vindas enviado para: " + usuario.getEmail());
    }

    @Operation(summary = "Enviar lembrete de devolução",
            description = "Envia um lembrete de devolução de livro para o usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lembrete enviado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/usuario/{usuarioId}/lembrete-devolucao")
    public ResponseEntity<String> enviarLembreteDevolucao(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId,
            @Parameter(description = "Título do livro") @RequestParam String tituloLivro,
            @Parameter(description = "Dias restantes para devolução") @RequestParam int diasRestantes) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        notificadorUsuario.enviarLembreteDevolucao(usuario, tituloLivro, diasRestantes);
        return ResponseEntity.ok("Lembrete de devolução enviado para: " + usuario.getEmail());
    }

    @Operation(summary = "Enviar notificação de atraso",
            description = "Envia uma notificação de atraso na devolução com informação de multa")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação enviada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/usuario/{usuarioId}/notificacao-atraso")
    public ResponseEntity<String> enviarNotificacaoAtraso(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId,
            @Parameter(description = "Título do livro") @RequestParam String tituloLivro,
            @Parameter(description = "Dias de atraso") @RequestParam int diasAtraso,
            @Parameter(description = "Valor da multa") @RequestParam BigDecimal valorMulta) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        notificadorUsuario.enviarNotificacaoAtraso(usuario, tituloLivro, diasAtraso, valorMulta);
        return ResponseEntity.ok("Notificação de atraso enviada para: " + usuario.getEmail());
    }

    @Operation(summary = "Enviar confirmação de devolução",
            description = "Envia confirmação de devolução de livro ao usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Confirmação enviada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/usuario/{usuarioId}/confirmacao-devolucao")
    public ResponseEntity<String> enviarConfirmacaoDevolucao(
            @Parameter(description = "ID do usuário") @PathVariable Long usuarioId,
            @Parameter(description = "Título do livro") @RequestParam String tituloLivro) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + usuarioId));

        notificadorUsuario.enviarConfirmacaoDevolucao(usuario, tituloLivro);
        return ResponseEntity.ok("Confirmação de devolução enviada para: " + usuario.getEmail());
    }
}