package com.estacionamento_api.estacionamento;

import com.estacionamento_api.estacionamento.service.UsuarioService;
import com.estacionamento_api.estacionamento.service.VagaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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

	/**
	 * Usuário admin padrão para dev/demo. Como o H2 é em memória (create-drop),
	 * ele precisa ser recriado a cada subida da aplicação — sem isso, ninguém
	 * conseguiria logar na primeira vez. Credenciais só para desenvolvimento;
	 * numa aplicação real isso não existiria (o cadastro seria feito via
	 * POST /auth/registrar).
	 */
	@Bean
	CommandLineRunner inicializarUsuarioAdmin(UsuarioService usuarioService) {
		return args -> {
			if (!usuarioService.existeUsuario("admin")) {
				usuarioService.registrar("admin", "admin123", "ADMIN");
				log.info("Usuário admin padrão criado (username=admin, senha=admin123 — só para dev!)");
			}
		};
	}

}
