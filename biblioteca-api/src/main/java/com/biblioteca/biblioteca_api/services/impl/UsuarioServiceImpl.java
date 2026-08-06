package com.biblioteca.biblioteca_api.services.impl;

import com.biblioteca.biblioteca_api.dto.request.UsuarioRequestDTO;
import com.biblioteca.biblioteca_api.dto.response.EstatisticasDTO;
import com.biblioteca.biblioteca_api.dto.response.UsuarioResponseDTO;
import com.biblioteca.biblioteca_api.entities.Emprestimo;
import com.biblioteca.biblioteca_api.entities.Usuario;
import com.biblioteca.biblioteca_api.enums.StatusEmprestimo;
import com.biblioteca.biblioteca_api.exceptions.BusinessException;
import com.biblioteca.biblioteca_api.exceptions.ResourceNotFoundException;
import com.biblioteca.biblioteca_api.mappers.UsuarioMapper;
import com.biblioteca.biblioteca_api.repositories.EmprestimoRepository;
import com.biblioteca.biblioteca_api.repositories.UsuarioRepository;
import com.biblioteca.biblioteca_api.services.GerenciadorUsuario;
import com.biblioteca.biblioteca_api.services.NotificadorUsuario;
import com.biblioteca.biblioteca_api.services.RelatorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements GerenciadorUsuario, NotificadorUsuario, RelatorioUsuario {

    private final UsuarioRepository usuarioRepository;
    private final EmprestimoRepository emprestimoRepository;

    // ==================== GERENCIADOR USUARIO ====================

    @Override
    @Transactional
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email já cadastrado.");
        }
        if (usuarioRepository.existsByCpf(dto.getCpf())) {
            throw new BusinessException("CPF já cadastrado.");
        }

        Usuario usuario = UsuarioMapper.toEntity(dto);

        usuario = usuarioRepository.save(usuario);
        enviarEmailBoasVindas(usuario);
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarUsuarioPorId(id);

        if (!usuario.getEmail().equals(dto.getEmail()) && usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email já cadastrado.");
        }
        if (!usuario.getCpf().equals(dto.getCpf()) && usuarioRepository.existsByCpf(dto.getCpf())) {
            throw new BusinessException("CPF já cadastrado.");
        }

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setCpf(dto.getCpf());
        usuario.setTipoUsuario(dto.getTipoUsuario());
        usuario = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);

        List<Emprestimo> emprestimosAtivos = emprestimoRepository.findByUsuarioAndStatus(usuario, StatusEmprestimo.ATIVO);

        if (!emprestimosAtivos.isEmpty()) {
            throw new BusinessException("Não é possível deletar usuário com empréstimos ativos");
        }

        usuarioRepository.delete(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com email: " + email));
        return UsuarioMapper.toResponseDTO(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return UsuarioMapper.toResponseDTOList(usuarioRepository.findAll());
    }

    // ==================== NOTIFICADOR USUARIO ====================

    @Override
    public void enviarEmailBoasVindas(Usuario usuario) {
        String assunto = "Bem-vindo à Biblioteca!";
        String mensagem = String.format(
                "Olá %s,\n\nSeja bem-vindo(a) à nossa biblioteca!\n\nSeu cadastro foi realizado com sucesso.\n\nAtenciosamente,\nEquipe Biblioteca",
                usuario.getNome()
        );
        enviarEmail(usuario.getEmail(), assunto, mensagem);
    }

    @Override
    public void enviarLembreteDevolucao(Usuario usuario, String tituloLivro, int diasRestantes) {
        String assunto = "Lembrete de Devolução";
        String mensagem = String.format(
                "Olá %s,\n\nEste é um lembrete de que o livro '%s' deve ser devolvido em %d dia(s).\n\nPor favor, não se esqueça de devolvê-lo no prazo para evitar multas.\n\nAtenciosamente,\nEquipe Biblioteca",
                usuario.getNome(),
                tituloLivro,
                diasRestantes
        );
        enviarEmail(usuario.getEmail(), assunto, mensagem);
    }

    @Override
    public void enviarNotificacaoAtraso(Usuario usuario, String tituloLivro, int diasAtraso, BigDecimal valorMulta) {
        String assunto = "Notificação de Atraso";
        String mensagem = String.format(
                "Olá %s,\n\nO livro '%s' está atrasado há %d dia(s).\n\nValor da multa acumulada: R$ %.2f\n\nPor favor, devolva o livro o quanto antes.\n\nAtenciosamente,\nEquipe Biblioteca",
                usuario.getNome(),
                tituloLivro,
                diasAtraso,
                valorMulta
        );
        enviarEmail(usuario.getEmail(), assunto, mensagem);
    }

    @Override
    public void enviarConfirmacaoDevolucao(Usuario usuario, String tituloLivro) {
        String assunto = "Confirmação de Devolução";
        String mensagem = String.format(
                "Olá %s,\n\nA devolução do livro '%s' foi confirmada com sucesso.\n\nObrigado por utilizar nossa biblioteca!\n\nAtenciosamente,\nEquipe Biblioteca",
                usuario.getNome(),
                tituloLivro
        );
        enviarEmail(usuario.getEmail(), assunto, mensagem);
    }

    // ==================== RELATORIO USUARIO ====================

    @Override
    @Transactional(readOnly = true)
    public byte[] gerarRelatorioEmprestimos(Long usuarioId) {
        Usuario usuario = buscarUsuarioPorId(usuarioId);
        List<Emprestimo> emprestimos = emprestimoRepository.findByUsuarioId(usuarioId);

        StringBuilder sb = new StringBuilder();
        sb.append("=== RELATÓRIO DE EMPRÉSTIMOS ===\n\n");
        sb.append("Usuário: ").append(usuario.getNome()).append("\n");
        sb.append("E-mail: ").append(usuario.getEmail()).append("\n");
        sb.append("CPF: ").append(usuario.getCpf()).append("\n\n");

        EstatisticasDTO stats = calcularEstatisticas(usuarioId);
        sb.append("Total de Empréstimos: ").append(stats.getTotalEmprestimos()).append("\n");
        sb.append("Empréstimos Ativos: ").append(stats.getEmprestimosAtivos()).append("\n");
        sb.append("Livros Devolvidos no Prazo: ").append(stats.getLivrosNoPrazo()).append("\n");
        sb.append("Livros Devolvidos com Atraso: ").append(stats.getLivrosAtrasados()).append("\n");
        sb.append("Multas Acumuladas: R$ ").append(String.format("%.2f", stats.getMultasAcumuladas())).append("\n\n");
        sb.append("========================================\n\n");

        sb.append("HISTÓRICO DE EMPRÉSTIMOS:\n\n");
        for (Emprestimo emp : emprestimos) {
            sb.append("Livro: ").append(emp.getLivro().getTitulo()).append("\n");
            sb.append("Data Empréstimo: ").append(emp.getDataEmprestimo()).append("\n");
            sb.append("Data Prevista: ").append(emp.getDataDevolucaoPrevista()).append("\n");
            sb.append("Data Real: ").append(emp.getDataDevolucaoReal() != null ? emp.getDataDevolucaoReal() : "Não devolvido").append("\n");
            sb.append("Status: ").append(emp.getStatus()).append("\n");
            sb.append("Multa: R$ ").append(String.format("%.2f", emp.getMultaCalculada())).append("\n");
            sb.append("----------------------------------------\n");
        }

        return sb.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public EstatisticasDTO calcularEstatisticas(Long usuarioId) {
        Usuario usuario = buscarUsuarioPorId(usuarioId);
        List<Emprestimo> emprestimos = emprestimoRepository.findByUsuarioId(usuarioId);

        long totalEmprestimos = emprestimos.size();

        long emprestimosAtivos = emprestimos.stream()
                .filter(e -> e.getStatus() == StatusEmprestimo.ATIVO)
                .count();

        long livrosAtrasados = emprestimos.stream()
                .filter(e -> e.getDataDevolucaoReal() != null
                        && e.getDataDevolucaoReal().isAfter(e.getDataDevolucaoPrevista()))
                .count();

        long livrosNoPrazo = emprestimos.stream()
                .filter(e -> e.getDataDevolucaoReal() != null
                        && !e.getDataDevolucaoReal().isAfter(e.getDataDevolucaoPrevista()))
                .count();

        BigDecimal multasAcumuladas = emprestimos.stream()
                .map(Emprestimo::getMultaCalculada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EstatisticasDTO.builder()
                .totalEmprestimos(totalEmprestimos)
                .emprestimosAtivos(emprestimosAtivos)
                .livrosAtrasados(livrosAtrasados)
                .livrosNoPrazo(livrosNoPrazo)
                .multasAcumuladas(multasAcumuladas)
                .build();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + id));
    }

    private void enviarEmail(String destinatario, String assunto, String mensagem) {
        System.out.println("===== ENVIANDO E-MAIL =====");
        System.out.println("Para: " + destinatario);
        System.out.println("Assunto: " + assunto);
        System.out.println("Mensagem:\n" + mensagem);
        System.out.println("===========================\n");
    }

}
