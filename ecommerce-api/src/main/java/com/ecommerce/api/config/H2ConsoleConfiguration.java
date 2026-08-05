package com.ecommerce.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração do H2 Console para ambiente de desenvolvimento
 * Spring Boot 4.1.0 com H2 2.4.240
 */
@Configuration
@Profile("!prod")
public class H2ConsoleConfiguration implements WebMvcConfigurer {
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redireciona /h2-console para /h2-console/
        registry.addRedirectViewController("/h2-console", "/h2-console/");
    }
}
