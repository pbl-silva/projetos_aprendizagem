package com.ecommerce.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Registrar módulo de LocalDateTime
        mapper.registerModule(new JavaTimeModule());
        
        // Configurar timezone
        mapper.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        
        // Desabilitar serialização de datas como timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Habilitar indentação (pretty print)
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Desabilitar erro em beans vazios
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        return mapper;
    }
}