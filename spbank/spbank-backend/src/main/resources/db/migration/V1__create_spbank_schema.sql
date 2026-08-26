CREATE TABLE instituicoes_bancarias (
  codigo_estudo CHAR(5) PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  interna TINYINT(1) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT chk_instituicao_codigo CHECK (codigo_estudo REGEXP '^[0-9]{5}$'),
  CONSTRAINT chk_instituicao_flags CHECK (interna IN (0,1) AND ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE contas (
  id CHAR(36) PRIMARY KEY,
  nome_titular VARCHAR(120) NOT NULL,
  documento_titular VARCHAR(14) NOT NULL,
  codigo_banco CHAR(5) NOT NULL,
  agencia VARCHAR(10) NOT NULL,
  numero_conta VARCHAR(20) NOT NULL,
  tipo_conta VARCHAR(20) NOT NULL,
  plano_conta VARCHAR(20) NOT NULL,
  saldo DECIMAL(19,2) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_conta_instituicao FOREIGN KEY (codigo_banco)
    REFERENCES instituicoes_bancarias(codigo_estudo),
  CONSTRAINT chk_tipo_conta CHECK (tipo_conta IN ('CORRENTE','POUPANCA')),
  CONSTRAINT chk_plano_conta CHECK (plano_conta IN ('STANDARD','PLUS')),
  CONSTRAINT chk_documento_titular CHECK (CHAR_LENGTH(documento_titular) IN (11,14)),
  CONSTRAINT chk_saldo_conta CHECK (saldo >= 0),
  CONSTRAINT chk_conta_ativa CHECK (ativa IN (0,1)),
  CONSTRAINT uk_dados_bancarios UNIQUE (codigo_banco, agencia, numero_conta, tipo_conta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE transferencias (
  id CHAR(36) PRIMARY KEY,
  tipo_transferencia VARCHAR(20) NOT NULL,
  conta_origem_id CHAR(36) NOT NULL,
  conta_destino_id CHAR(36) NULL,
  codigo_banco_destino CHAR(5) NOT NULL,
  nome_destinatario VARCHAR(120) NOT NULL,
  documento_destinatario VARCHAR(14) NOT NULL,
  agencia_destino VARCHAR(10) NOT NULL,
  numero_conta_destino VARCHAR(20) NOT NULL,
  tipo_conta_destino VARCHAR(20) NOT NULL,
  valor DECIMAL(19,2) NOT NULL,
  taxa DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  taxa_calculada TINYINT(1) NOT NULL DEFAULT 0,
  situacao VARCHAR(20) NOT NULL,
  chave_idempotencia CHAR(36) NOT NULL,
  hash_requisicao CHAR(64) NOT NULL,
  solicitada_em DATETIME(6) NOT NULL,
  agendada_para DATE NULL,
  processada_em DATETIME(6) NULL,
  referencia_liquidacao VARCHAR(100) NULL,
  codigo_falha VARCHAR(60) NULL,
  mensagem_falha VARCHAR(240) NULL,
  CONSTRAINT fk_transferencia_origem FOREIGN KEY (conta_origem_id) REFERENCES contas(id),
  CONSTRAINT fk_transferencia_destino FOREIGN KEY (conta_destino_id) REFERENCES contas(id),
  CONSTRAINT fk_transferencia_instituicao FOREIGN KEY (codigo_banco_destino)
    REFERENCES instituicoes_bancarias(codigo_estudo),
  CONSTRAINT uk_transferencia_idempotencia UNIQUE (conta_origem_id, chave_idempotencia),
  CONSTRAINT chk_tipo_transferencia CHECK (tipo_transferencia IN ('INTERNA','TED')),
  CONSTRAINT chk_destino_por_modalidade CHECK (
    (tipo_transferencia = 'INTERNA' AND conta_destino_id IS NOT NULL)
    OR (tipo_transferencia = 'TED' AND conta_destino_id IS NULL)
  ),
  CONSTRAINT chk_valor_transferencia CHECK (valor > 0),
  CONSTRAINT chk_taxa_transferencia CHECK (taxa >= 0),
  CONSTRAINT chk_taxa_calculada CHECK (taxa_calculada IN (0,1)),
  CONSTRAINT chk_hash_requisicao CHECK (hash_requisicao REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_situacao_transferencia CHECK (
    situacao IN ('AGENDADA','PROCESSANDO','CONCLUIDA','FALHA','CANCELADA')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE lancamentos_conta (
  id CHAR(36) PRIMARY KEY,
  conta_id CHAR(36) NOT NULL,
  referencia_id CHAR(36) NOT NULL,
  tipo_referencia VARCHAR(30) NOT NULL,
  tipo_lancamento VARCHAR(30) NOT NULL,
  valor DECIMAL(19,2) NOT NULL,
  natureza VARCHAR(10) NOT NULL,
  saldo_apos DECIMAL(19,2) NOT NULL,
  descricao VARCHAR(180) NOT NULL,
  ocorrido_em DATETIME(6) NOT NULL,
  nome_contraparte VARCHAR(120) NULL,
  banco_contraparte VARCHAR(8) NULL,
  modalidade_operacao VARCHAR(20) NULL,
  CONSTRAINT fk_lancamento_conta FOREIGN KEY (conta_id) REFERENCES contas(id),
  CONSTRAINT uk_lancamento_referencia UNIQUE (
    referencia_id,
    tipo_referencia,
    conta_id,
    tipo_lancamento
  ),
  CONSTRAINT chk_tipo_referencia CHECK (
    tipo_referencia IN ('TRANSFERENCIA','PIX','CARTAO','INVESTIMENTO')
  ),
  CONSTRAINT chk_tipo_lancamento CHECK (
    tipo_lancamento IN ('TRANSFERENCIA_SAIDA','TRANSFERENCIA_ENTRADA','TAXA')
  ),
  CONSTRAINT chk_valor_lancamento CHECK (valor > 0),
  CONSTRAINT chk_natureza_lancamento CHECK (natureza IN ('DEBITO','CREDITO')),
  CONSTRAINT chk_saldo_apos_lancamento CHECK (saldo_apos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credenciais_acesso (
  conta_id CHAR(36) PRIMARY KEY,
  usuario VARCHAR(60) NOT NULL,
  senha_hash VARCHAR(180) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_credencial_conta FOREIGN KEY (conta_id) REFERENCES contas(id),
  CONSTRAINT uk_credencial_usuario UNIQUE (usuario),
  CONSTRAINT chk_credencial_ativa CHECK (ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sessoes_acesso (
  id CHAR(36) PRIMARY KEY,
  conta_id CHAR(36) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  criada_em DATETIME(6) NOT NULL,
  expira_em DATETIME(6) NOT NULL,
  revogada_em DATETIME(6) NULL,
  CONSTRAINT fk_sessao_conta FOREIGN KEY (conta_id) REFERENCES contas(id),
  CONSTRAINT uk_sessao_token UNIQUE (token_hash),
  CONSTRAINT chk_expiracao_sessao CHECK (expira_em > criada_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_instituicoes_ativas
  ON instituicoes_bancarias(ativa, nome);

CREATE INDEX idx_transferencias_pendentes
  ON transferencias(situacao, agendada_para);

CREATE INDEX idx_transferencias_origem_data
  ON transferencias(conta_origem_id, solicitada_em);

CREATE INDEX idx_lancamentos_conta_data
  ON lancamentos_conta(conta_id, ocorrido_em DESC);

CREATE INDEX idx_sessoes_conta_expiracao
  ON sessoes_acesso(conta_id, expira_em);