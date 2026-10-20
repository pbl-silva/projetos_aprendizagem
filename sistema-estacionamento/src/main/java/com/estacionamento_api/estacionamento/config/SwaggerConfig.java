package com.estacionamento_api.estacionamento.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI estacionamentoOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API de Estacionamento")
                .description("API de Gerenciamento de Estacionamento: vagas, veículos, entradas/saídas e relatórios")
                .version("v1"));
    }
}
