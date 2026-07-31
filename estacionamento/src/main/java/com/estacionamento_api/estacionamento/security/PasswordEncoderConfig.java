package com.estacionamento_api.estacionamento.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * O PasswordEncoder fica isolado numa classe própria, sem nenhuma
 * dependência, de propósito. Se ele estivesse dentro do SecurityConfig,
 * teríamos um ciclo: SecurityConfig depende de JwtAuthenticationFilter,
 * que depende de UsuarioService, que depende de PasswordEncoder — que
 * seria produzido pelo próprio SecurityConfig. O Spring não consegue
 * resolver isso (BeanCurrentlyInCreationException na subida da aplicação).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
