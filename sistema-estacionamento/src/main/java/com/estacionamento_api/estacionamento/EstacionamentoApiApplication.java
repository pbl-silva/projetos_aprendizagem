package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.service.UsuarioService;
import com.estacionamento_api.estacionamento.service.VagaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@SpringBootApplication
public class EstacionamentoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EstacionamentoApiApplication.class, args);
	}

	@Bean
	CommandLineRunner inicializarDados(VagaService vagaService) {
		return args -> vagaService.inicializarVagas();
	}

	/** Cria a conta inicial de desenvolvimento apenas quando ainda não existe administrador. */
	@Bean
	@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
	CommandLineRunner inicializarUsuarioAdmin(
			UsuarioService usuarioService,
			@Value("${app.bootstrap-admin.username}") String username,
			@Value("${app.bootstrap-admin.password}") String password) {
		return args -> {
            if (!usuarioService.existeAdministrador()) {
				usuarioService.registrar(username, password, "ADMIN");
				log.info("Usuário administrador inicial criado: {}", username);
			}
		};
	}

}
