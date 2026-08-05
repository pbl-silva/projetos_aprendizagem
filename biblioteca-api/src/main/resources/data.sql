-- ========================================
-- LIVROS (5 categorias diferentes)
-- ========================================
INSERT INTO livro (titulo, isbn, autor, ano_publicacao, categoria, disponivel) VALUES
('Dom Casmurro', '9788535902778', 'Machado de Assis', 1899, 'ROMANCE', true),
('1984', '9788535914849', 'George Orwell', 1949, 'FICCAO', false),
('Sapiens: Uma Breve História da Humanidade', '9788525432186', 'Yuval Noah Harari', 2011, 'NAO_FICCAO', false),
('Clean Code', '9780132350884', 'Robert C. Martin', 2008, 'TECNICO', true),
('O Silêncio dos Inocentes', '9788501061233', 'Thomas Harris', 1988, 'SUSPENSE', true);

-- ========================================
-- USUARIOS (1 COMUM, 2 PREMIUM)
-- ========================================
INSERT INTO usuario (nome, email, cpf, tipo_usuario, data_cadastro) VALUES
('Ananias Nicolau', 'ananias.nicolau@email.com', '12345678901', 'COMUM', '2025-01-15'),
('Bruno Costa Lima', 'bruno.lima@email.com', '98765432100', 'PREMIUM', '2024-11-03'),
('Carla Mendes', 'carla.mendes@email.com', '45612378900', 'PREMIUM', '2025-03-20');

-- ========================================
-- EMPRESTIMOS (2 ativos, 1 devolvido)
-- ========================================

-- Ativo, dentro do prazo (Bruno - PREMIUM, livro 1984)
INSERT INTO emprestimo (livro_id, usuario_id, data_emprestimo, data_devolucao_prevista, data_devolucao_real, status, multa_calculada) VALUES
(2, 2, '2026-06-15', '2026-06-29', NULL, 'ATIVO', 0.00);

-- Ativo, mas já atrasado (Ananias - COMUM, livro Sapiens)
INSERT INTO emprestimo (livro_id, usuario_id, data_emprestimo, data_devolucao_prevista, data_devolucao_real, status, multa_calculada) VALUES
(3, 1, '2026-06-05', '2026-06-12', NULL, 'ATIVO', 0.00);

-- Devolvido antes do prazo (Carla - PREMIUM, livro Silêncio dos Inocentes)
INSERT INTO emprestimo (livro_id, usuario_id, data_emprestimo, data_devolucao_prevista, data_devolucao_real, status, multa_calculada) VALUES
(5, 3, '2026-05-20', '2026-06-03', '2026-06-01', 'DEVOLVIDO', 0.00);