package com.biblioteca.biblioteca_api;

import com.biblioteca.biblioteca_api.dto.request.EmprestimoRequestDTO;
import com.biblioteca.biblioteca_api.entities.Livro;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.TipoUsuario;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.repositories.EmprestimoRepository;
import com.biblioteca.biblioteca_api.repositories.LivroRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import com.biblioteca.biblioteca_api.services.impl.EmprestimoServiceImpl;
import com.biblioteca.biblioteca_api.strategy.CalculadoraMulta;
import com.biblioteca.biblioteca_api.strategy.CalculadoraMultaComum;
import com.biblioteca.biblioteca_api.strategy.CalculadoraMultaPremium;
import com.biblioteca.biblioteca_api.validators.EmprestimoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmprestimoServiceTest {

    @Mock private EmprestimoRepository emprestimoRepository;
    @Mock private LivroRepository livroRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmprestimoValidator validator;

    private EmprestimoServiceImpl service;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Instant instant = Instant.parse("2026-06-19T03:00:00Z"); // 2026-06-19 00:00 America/Sao_Paulo
        fixedClock = Clock.fixed(instant, ZoneId.of("America/Sao_Paulo"));

        // constrói service com todos os mocks (evita NPEs)
        service = new EmprestimoServiceImpl(emprestimoRepository, livroRepository, usuarioRepository, validator, fixedClock);
    }

    @Test
    void deveRealizarEmprestimoComSucesso() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setDisponivel(true);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.COMUM);

        when(livroRepository.findById(1L)).thenReturn(java.util.Optional.of(livro));
        when(usuarioRepository.findById(1L)).thenReturn(java.util.Optional.of(usuario));
        when(emprestimoRepository.contarEmprestimosAtivosPorUsuario(usuario)).thenReturn(0L);
        when(emprestimoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmprestimoRequestDTO dto = new EmprestimoRequestDTO(1L, 1L);

        doNothing().when(validator).validarLivroDisponivel(livro);
        doNothing().when(validator).validarLimiteEmprestimos(usuario, 0L);

        var resp = service.realizarEmprestimo(dto);

        assertNotNull(resp);
        assertEquals(LocalDate.of(2026, 6, 19), resp.getDataEmprestimo());
        assertEquals(LocalDate.of(2026, 6, 19).plusDays(usuario.getTipoUsuario().getDiasPrazo()), resp.getDataDevolucaoPrevista());
        verify(validator).validarLivroDisponivel(livro);
        verify(validator).validarLimiteEmprestimos(usuario, 0L);
        verify(livroRepository).save(livro);
    }

    @Test
    void deveLancarExcecaoQuandoLivroIndisponivel() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setDisponivel(false);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.COMUM);

        when(livroRepository.findById(1L)).thenReturn(java.util.Optional.of(livro));
        when(usuarioRepository.findById(1L)).thenReturn(java.util.Optional.of(usuario));
        when(emprestimoRepository.contarEmprestimosAtivosPorUsuario(usuario)).thenReturn(0L);

        // fazer o validator lançar BusinessException ao validar livro indisponível
        doThrow(new BusinessException("Livro indisponível")).when(validator).validarLivroDisponivel(livro);

        EmprestimoRequestDTO dto = new EmprestimoRequestDTO(1L, 1L);

        var ex = assertThrows(BusinessException.class, () -> service.realizarEmprestimo(dto));
        assertEquals("Livro indisponível", ex.getMessage());

        verify(validator).validarLivroDisponivel(livro);
        verify(validator, never()).validarLimiteEmprestimos(any(), anyLong());
    }

    @Test
    void deveLancarExcecaoQuandoExcedeuLimite() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setDisponivel(true);

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(TipoUsuario.COMUM);

        when(livroRepository.findById(1L)).thenReturn(java.util.Optional.of(livro));
        when(usuarioRepository.findById(1L)).thenReturn(java.util.Optional.of(usuario));

        long limite = usuario.getTipoUsuario().getMaximoEmprestimos();
        // simula que usuário já tem o número máximo de empréstimos
        when(emprestimoRepository.contarEmprestimosAtivosPorUsuario(usuario)).thenReturn(limite);

        // validator deve lançar BusinessException quando validar limite
        doThrow(new BusinessException("Limite excedido"))
                .when(validator).validarLimiteEmprestimos(usuario, limite);

        EmprestimoRequestDTO dto = new EmprestimoRequestDTO(1L, 1L);

        var ex = assertThrows(BusinessException.class, () -> service.realizarEmprestimo(dto));
        assertEquals("Limite excedido", ex.getMessage());

        verify(validator).validarLivroDisponivel(livro);
        verify(validator).validarLimiteEmprestimos(usuario, limite);
    }

    @Test
    void deveCalcularMultaCorretamenteAoDevolverAtrasado() {
        Livro livro = new Livro();
        livro.setId(10L);
        livro.setDisponivel(false);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.COMUM);

        var emprestimo = com.biblioteca.biblioteca_api.entities.Emprestimo.builder()
                .id(10L)
                .livro(livro)
                .usuario(usuario)
                .dataEmprestimo(LocalDate.of(2026,6,1))
                .dataDevolucaoPrevista(LocalDate.of(2026,6,8))
                .status(com.biblioteca.biblioteca_api.enums.StatusEmprestimo.ATIVO)
                .build();

        when(emprestimoRepository.findById(10L)).thenReturn(java.util.Optional.of(emprestimo));
        when(emprestimoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(validator).validarEmprestimoAtivo(emprestimo);

        var resp = service.devolverLivro(10L);

        long diasAtraso = 11; // 2026-06-19 - 2026-06-08
        long diasTolerancia = usuario.getTipoUsuario().getDiasTolerancia();
        long diasMultados = Math.max(0L, diasAtraso - diasTolerancia);
        java.math.BigDecimal valorEsperado = new java.math.BigDecimal("2.00")
                .multiply(java.math.BigDecimal.valueOf(diasMultados));

        assertNotNull(resp);
        assertEquals(LocalDate.of(2026,6,19), resp.getDataDevolucaoReal());
        assertEquals(valorEsperado, resp.getMultaCalculada());
        verify(validator).validarEmprestimoAtivo(emprestimo);
        verify(livroRepository).save(livro);
    }

    @Test
    void deveCalcularMultaCorretamenteParaUsuarioPremium() {
        // Regressão do bug: tolerância de 5 dias para PREMIUM era descontada duas vezes
        // (uma em EmprestimoServiceImpl.calcularMulta() e outra em CalculadoraMultaPremium),
        // fazendo um atraso de 11 dias gerar R$2,00 em vez dos R$12,00 corretos.
        Map<String, CalculadoraMulta> calculadoras = Map.of(
                "COMUM", new CalculadoraMultaComum(),
                "PREMIUM", new CalculadoraMultaPremium()
        );
        EmprestimoServiceImpl servicePremium = new EmprestimoServiceImpl(
                emprestimoRepository, livroRepository, usuarioRepository, validator, fixedClock, calculadoras);

        Livro livro = new Livro();
        livro.setId(20L);
        livro.setDisponivel(false);

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setTipoUsuario(TipoUsuario.PREMIUM);

        var emprestimo = com.biblioteca.biblioteca_api.entities.Emprestimo.builder()
                .id(20L)
                .livro(livro)
                .usuario(usuario)
                .dataEmprestimo(LocalDate.of(2026, 6, 1))
                .dataDevolucaoPrevista(LocalDate.of(2026, 6, 8)) // 11 dias de atraso até 2026-06-19
                .status(com.biblioteca.biblioteca_api.enums.StatusEmprestimo.ATIVO)
                .build();

        when(emprestimoRepository.findById(20L)).thenReturn(java.util.Optional.of(emprestimo));
        when(emprestimoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(validator).validarEmprestimoAtivo(emprestimo);

        var resp = servicePremium.devolverLivro(20L);

        // 11 dias de atraso - 5 dias de tolerância PREMIUM = 6 dias multados x R$2,00 = R$12,00
        assertEquals(new java.math.BigDecimal("12.00"), resp.getMultaCalculada());
    }

    @Test
    void respostaDeveIncluirDadosCompletosDoLivroEUsuario() {
        Livro livro = new Livro();
        livro.setId(30L);
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert C. Martin");
        livro.setDisponivel(true);

        Usuario usuario = new Usuario();
        usuario.setId(30L);
        usuario.setNome("Ana");
        usuario.setEmail("ana@email.com");
        usuario.setCpf("99988877766");
        usuario.setTipoUsuario(TipoUsuario.COMUM);

        when(livroRepository.findById(30L)).thenReturn(java.util.Optional.of(livro));
        when(usuarioRepository.findById(30L)).thenReturn(java.util.Optional.of(usuario));
        when(emprestimoRepository.contarEmprestimosAtivosPorUsuario(usuario)).thenReturn(0L);
        when(emprestimoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(validator).validarLivroDisponivel(livro);
        doNothing().when(validator).validarLimiteEmprestimos(usuario, 0L);

        var resp = service.realizarEmprestimo(new EmprestimoRequestDTO(30L, 30L));

        assertNotNull(resp.getLivro());
        assertEquals("Clean Code", resp.getLivro().getTitulo());
        assertEquals("Robert C. Martin", resp.getLivro().getAutor());

        assertNotNull(resp.getUsuario());
        assertEquals("Ana", resp.getUsuario().getNome());
        assertEquals("ana@email.com", resp.getUsuario().getEmail());
    }
}