package com.biblioteca.biblioteca_api.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
public class SistemaController {
    private final ConfigurableApplicationContext context;
    private final String shutdownToken;

    public SistemaController(ConfigurableApplicationContext context,
            @Value("${BIBLIOTECA_SHUTDOWN_TOKEN:}") String shutdownToken) {
        this.context = context;
        this.shutdownToken = shutdownToken;
    }

    @GetMapping("/api/sistema")
    public Map<String, String> identificar() {
        return Map.of("aplicacao", "biblioteca");
    }

    @PostMapping("/api/sistema/encerrar")
    public ResponseEntity<Void> encerrar(@RequestHeader(value = "X-Iniciador-Token", defaultValue = "") String token) {
        if (shutdownToken.isBlank() || !MessageDigest.isEqual(shutdownToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) return ResponseEntity.status(403).build();
        Thread encerramento = new Thread(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            context.close();
        }, "biblioteca-encerramento");
        encerramento.start();
        return ResponseEntity.accepted().build();
    }
}
