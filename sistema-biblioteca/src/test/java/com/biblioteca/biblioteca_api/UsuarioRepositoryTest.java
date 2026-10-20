    package com.biblioteca.biblioteca_api;

    import static org.junit.jupiter.api.Assertions.*;

    import com.biblioteca.biblioteca_api.entities.Usuario;
    import com.biblioteca.biblioteca_api.enums.TipoUsuario;
    import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
    import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

    import java.time.LocalDate;
    import java.util.Optional;
    import java.util.UUID;
    import java.util.concurrent.ThreadLocalRandom;

    @DataJpaTest
    @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
    class UsuarioRepositoryTest {

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Test
        @DisplayName("Deve buscar usuário por email")
        void deveBuscarPorEmail() {
            String unique = UUID.randomUUID().toString();
            Usuario u = Usuario.builder()
                    .nome("BuscaEmail")
                    .email("busca.email+" + unique + "@test.com")
                    .cpf(randomCpf())
                    .tipoUsuario(TipoUsuario.COMUM)
                    .dataCadastro(LocalDate.now())
                    .build();
            usuarioRepository.save(u);

            Optional<Usuario> opt = usuarioRepository.findByEmail(u.getEmail());
            assertTrue(opt.isPresent());
            assertEquals("BuscaEmail", opt.get().getNome());
        }

        @Test
        @DisplayName("Deve buscar usuário por CPF")
        void deveBuscarPorCpf() {
            String cpf = randomCpf();
            Usuario u = Usuario.builder()
                    .nome("BuscaCpf")
                    .email("cpf.test+" + UUID.randomUUID().toString() + "@test.com")
                    .cpf(cpf)
                    .tipoUsuario(TipoUsuario.COMUM)
                    .dataCadastro(LocalDate.now())
                    .build();
            usuarioRepository.save(u);

            Optional<Usuario> opt = usuarioRepository.findByCpf(cpf);
            assertTrue(opt.isPresent());
            assertEquals("BuscaCpf", opt.get().getNome());
        }

        private String randomCpf() {
            // gera 11 dígitos aleatórios simples (não valida dígitos verificadores)
            StringBuilder sb = new StringBuilder();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 11; i++) sb.append(r.nextInt(0, 10));
            return sb.toString();
        }
    }