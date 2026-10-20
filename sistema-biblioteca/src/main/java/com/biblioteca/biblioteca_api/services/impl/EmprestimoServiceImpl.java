package com.biblioteca.biblioteca_api.services.impl;

import com.biblioteca.biblioteca_api.dto.request.EmprestimoRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.EmprestimoResponseDTO;
import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import com.biblioteca.biblioteca_api.exceptions.ResourceNotFoundException;
import com.biblioteca.biblioteca_api.mappers.EmprestimoMapper;
import com.biblioteca.biblioteca_api.repositories.EmprestimoRepository;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import com.biblioteca.biblioteca_api.services.GerenciadorEmprestimo;
import com.biblioteca.biblioteca_api.strategy.CalculadoraMulta;
import com.biblioteca.biblioteca_api.validators.EmprestimoValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EmprestimoServiceImpl implements GerenciadorEmprestimo {

    private final EmprestimoRepository emprestimoRepository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmprestimoValidator validator;
    private final Clock clock;
    private final Map<String, CalculadoraMulta> calculadoras;

    @Autowired
    public EmprestimoServiceImpl(EmprestimoRepository emprestimoRepository,
                                 LivroRepository livroRepository,
                                 UsuarioRepository usuarioRepository,
                                 EmprestimoValidator validator,
                                 Clock clock,
                                 Map<String, CalculadoraMulta> calculadoras) {
        this.emprestimoRepository = Objects.requireNonNull(emprestimoRepository);
        this.livroRepository = Objects.requireNonNull(livroRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.validator = Objects.requireNonNull(validator);
        this.clock = Objects.requireNonNull(clock);

        if (calculadoras == null || calculadoras.isEmpty()) {
            Map<String, CalculadoraMulta> padrao = new HashMap<>();
            padrao.put(TipoUsuario.COMUM.name(), dias -> BigDecimal.valueOf(dias).multiply(new BigDecimal("2.00")));
            this.calculadoras = Collections.unmodifiableMap(padrao);
        } else {
            this.calculadoras = Collections.unmodifiableMap(new HashMap<>(calculadoras));
        }
    }

    public EmprestimoServiceImpl(EmprestimoRepository emprestimoRepository,
                                 LivroRepository livroRepository,
                                 UsuarioRepository usuarioRepository,
                                 EmprestimoValidator validator,
                                 Clock clock) {
        this(emprestimoRepository, livroRepository, usuarioRepository, validator, clock, Collections.emptyMap());
    }

    @Override
    @Transactional
    public EmprestimoResponseDTO realizarEmprestimo(EmprestimoRequestDTO dto) {
        Livro livro = livroRepository.findById(dto.getLivroId())
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado com ID: " + dto.getLivroId()));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + dto.getUsuarioId()));

        validator.validarLivroDisponivel(livro);

        long emprestimosAtivos = emprestimoRepository.contarEmprestimosAtivosPorUsuario(usuario);
        validator.validarLimiteEmprestimos(usuario, emprestimosAtivos);

        LocalDate hoje = LocalDate.now(clock);
        Emprestimo emprestimo = Emprestimo.builder()
                .livro(livro)
                .usuario(usuario)
                .dataEmprestimo(hoje)
                .dataDevolucaoPrevista(hoje.plusDays(usuario.getTipoUsuario().getDiasPrazo()))
                .status(StatusEmprestimo.ATIVO)
                .build();

        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        livro.setDisponivel(false);
        livroRepository.save(livro);
        return EmprestimoMapper.toResponseDTO(salvo, clock);
    }

    @Override
    @Transactional
    public EmprestimoResponseDTO devolverLivro(Long emprestimoId) {
        Emprestimo emprestimo = emprestimoRepository.findById(emprestimoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado com ID: " + emprestimoId));

        validator.validarEmprestimoAtivo(emprestimo);

        LocalDate hoje = LocalDate.now(clock);
        emprestimo.setDataDevolucaoReal(hoje);

        BigDecimal multa = calcularMulta(emprestimo);
        emprestimo.setMultaCalculada(multa);

        emprestimo.setStatus(StatusEmprestimo.DEVOLVIDO);
        Emprestimo salvo = emprestimoRepository.save(emprestimo);

        Livro livro = emprestimo.getLivro();
        livro.setDisponivel(true);
        livroRepository.save(livro);

        return EmprestimoMapper.toResponseDTO(salvo, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarTodos() {
        return EmprestimoMapper.toResponseDTOList(emprestimoRepository.findAll(), clock);
    }

    @Override
    @Transactional(readOnly = true)
    public EmprestimoResponseDTO buscarPorId(Long id) {
        Emprestimo emprestimo = emprestimoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado com ID: " + id));
        return EmprestimoMapper.toResponseDTO(emprestimo, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarPorUsuario(Long usuarioId) {
        List<Emprestimo> lista = emprestimoRepository.findByUsuarioId(usuarioId);
        return EmprestimoMapper.toResponseDTOList(lista, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarAtivos() {
        List<Emprestimo> lista = emprestimoRepository.findByStatus(StatusEmprestimo.ATIVO);
        return EmprestimoMapper.toResponseDTOList(lista, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmprestimoResponseDTO> listarAtrasados() {
        LocalDate hoje = LocalDate.now(clock);
        List<Emprestimo> lista = emprestimoRepository.buscarEmprestimosAtrasados(hoje);
        return EmprestimoMapper.toResponseDTOList(lista, clock);
    }

    private BigDecimal calcularMulta(Emprestimo emprestimo) {
        long diasDeAtraso = calcularDiasDeAtraso(emprestimo);
        if (diasDeAtraso <= 0) {
            return BigDecimal.ZERO;
        }

        TipoUsuario tipo = emprestimo.getUsuario().getTipoUsuario();
        long diasTolerancia = tipo.getDiasTolerancia();
        long diasMultados = Math.max(0L, diasDeAtraso - diasTolerancia);

        if (diasMultados <= 0) {
            return BigDecimal.ZERO;
        }

        CalculadoraMulta calculadora = calculadoras.get(tipo.name());
        if (calculadora == null) {
            throw new IllegalStateException("Nenhuma CalculadoraMulta registrada para tipo: " + tipo.name());
        }
        return calculadora.calcular(diasMultados);
    }

    private long calcularDiasDeAtraso(Emprestimo emprestimo) {
        LocalDate devolucaoReal = emprestimo.getDataDevolucaoReal();
        LocalDate referencia = (devolucaoReal != null) ? devolucaoReal : LocalDate.now(clock);
        LocalDate prevista = emprestimo.getDataDevolucaoPrevista();
        long diff = ChronoUnit.DAYS.between(prevista, referencia);
        return Math.max(0L, diff);
    }

}
