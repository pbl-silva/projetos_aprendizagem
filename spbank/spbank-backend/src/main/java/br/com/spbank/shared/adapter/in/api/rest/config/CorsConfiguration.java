package br.com.spbank.shared.adapter.in.api.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration
        implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfiguration(
            @Value("${spbank.cors.allowed-origins}")
            String[] allowedOrigins
    ) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(
            CorsRegistry registry
    ) {
        registry
                .addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE"
                )
                .allowedHeaders(
                        "Content-Type",
                        "Idempotency-Key",
                        "Authorization",
                        "X-Correlation-Id"
                );
    }
}