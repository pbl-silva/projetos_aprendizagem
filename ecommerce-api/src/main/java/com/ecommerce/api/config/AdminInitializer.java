package com.ecommerce.api.config;

import com.ecommerce.api.model.Usuario;
import com.ecommerce.api.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true")
public class AdminInitializer {

    @Bean
    CommandLineRunner criarAdministrador(UsuarioRepository usuarioRepository,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${app.admin.email:}") String adminEmail,
                                         @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            if (adminEmail.isBlank() || adminPassword.length() < 8) {
                throw new IllegalStateException(
                    "Defina APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD (minimo de 8 caracteres) " +
                    "quando APP_ADMIN_ENABLED=true"
                );
            }

            if (usuarioRepository.existsByEmail(adminEmail)) {
                return;
            }

            Usuario administrador = Usuario.builder()
                .email(adminEmail)
                .senha(passwordEncoder.encode(adminPassword))
                .nome("Administrador")
                .papel(Usuario.Papel.ADMIN)
                .ativo(true)
                .build();

            usuarioRepository.save(administrador);
        };
    }
}
