package com.biblioteca.biblioteca_api.services;

import com.biblioteca.biblioteca_api.entities.ImportacaoAcervo;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import com.biblioteca.biblioteca_api.repositories.ImportacaoAcervoRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LeitoresIniciais implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final ImportacaoAcervoRepository importacoes;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (importacoes.existsById("leitores-v1")) return;
        incluir("Ananias Nicolau", "ananias.nicolau@email.com", "12345678901", TipoUsuario.COMUM);
        incluir("Bruno Costa Lima", "bruno.lima@email.com", "98765432100", TipoUsuario.PREMIUM);
        incluir("Carla Mendes", "carla.mendes@email.com", "45612378900", TipoUsuario.PREMIUM);
        importacoes.save(new ImportacaoAcervo("leitores-v1"));
    }

    private void incluir(String nome, String email, String cpf, TipoUsuario tipo) {
        if (usuarios.existsByEmail(email) || usuarios.existsByCpf(cpf)) return;
        usuarios.save(Usuario.builder().nome(nome).email(email).cpf(cpf).tipoUsuario(tipo).build());
    }
}
