INSERT INTO instituicoes_bancarias (codigo_estudo, nome, interna, ativa) VALUES
('90001', 'SPBank', 1, 1),
('90002', 'Banco Aurora', 0, 1),
('90003', 'Banco Horizonte', 0, 1),
('90004', 'Banco Nexo', 0, 1),
('90005', 'Banco Solaris', 0, 1);

INSERT INTO contas (
  id, nome_titular, documento_titular, codigo_banco, agencia,
  numero_conta, tipo_conta, plano_conta, saldo, ativa
) VALUES
  ('11111111-1111-1111-1111-111111111111', 'João Souza',
   '52998224725', '90001', '0001', '100010', 'CORRENTE', 'PLUS', 5000.00, 1),
  ('22222222-2222-2222-2222-222222222222', 'Maria Silva',
   '11144477735', '90001', '0001', '200020', 'CORRENTE', 'STANDARD', 1000.00, 1);

INSERT INTO credenciais_acesso (conta_id, usuario, senha_hash, ativa) VALUES
  ('11111111-1111-1111-1111-111111111111', 'joao',
   'pbkdf2_sha256$120000$Y7Ewt8u4xxUhkqwv10kruw==$4fWSAGaJDZs4+xkmx7e/40r66Y9WDIdnl+Cb5x71hwk=', 1),
  ('22222222-2222-2222-2222-222222222222', 'maria',
   'pbkdf2_sha256$120000$OZ5qta4vO4jGz8dWMBKn7Q==$MAIDChkVoF4Q8N/K1xlCmZfCCAClUn1SwUia21wtXCo=', 1);