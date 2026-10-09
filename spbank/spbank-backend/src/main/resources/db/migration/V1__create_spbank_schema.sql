CREATE TABLE instituicoes_bancarias (
  codigo_estudo CHAR(5) PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  interna TINYINT(1) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT chk_instituicao_codigo CHECK (codigo_estudo REGEXP '^[0-9]{5}$'),
  CONSTRAINT chk_instituicao_flags CHECK (interna IN (0,1) AND ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clientes (
  id CHAR(36) PRIMARY KEY,
  nome_completo VARCHAR(120) NOT NULL,
  cpf CHAR(11) NOT NULL,
  data_nascimento DATE NOT NULL,
  celular CHAR(11) NOT NULL,
  email VARCHAR(254) NOT NULL,
  cep CHAR(8) NOT NULL,
  logradouro VARCHAR(120) NOT NULL,
  numero_endereco VARCHAR(20) NOT NULL,
  complemento VARCHAR(80) NULL,
  bairro VARCHAR(80) NOT NULL,
  cidade VARCHAR(80) NOT NULL,
  uf CHAR(2) NOT NULL,
  plano_cliente VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
  ativo TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT chk_plano_cliente CHECK (plano_cliente IN ('STANDARD','PLUS')),
  CONSTRAINT uk_cliente_cpf UNIQUE (cpf),
  CONSTRAINT chk_cliente_cpf CHECK (cpf REGEXP '^[0-9]{11}$'),
  CONSTRAINT chk_cliente_nascimento CHECK (data_nascimento >= '1900-01-01'),
  CONSTRAINT chk_cliente_celular CHECK (celular REGEXP '^[1-9][0-9]9[0-9]{8}$'),
  CONSTRAINT chk_cliente_email CHECK (email LIKE '%_@_%._%'),
  CONSTRAINT chk_cliente_cep CHECK (cep REGEXP '^[0-9]{8}$'),
  CONSTRAINT chk_cliente_uf CHECK (uf REGEXP '^[A-Z]{2}$'),
  CONSTRAINT chk_cliente_ativo CHECK (ativo IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE contas (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  nome_titular VARCHAR(120) NOT NULL,
  documento_titular VARCHAR(14) NOT NULL,
  codigo_banco CHAR(5) NOT NULL,
  agencia VARCHAR(10) NOT NULL,
  numero_conta VARCHAR(20) NOT NULL,
  tipo_conta VARCHAR(20) NOT NULL,
  saldo DECIMAL(19,2) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_conta_instituicao FOREIGN KEY (codigo_banco)
    REFERENCES instituicoes_bancarias(codigo_estudo),
  CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT chk_tipo_conta CHECK (tipo_conta IN ('CORRENTE','POUPANCA')),
  CONSTRAINT chk_documento_titular CHECK (CHAR_LENGTH(documento_titular) IN (11,14)),
  CONSTRAINT chk_saldo_conta CHECK (saldo >= 0),
  CONSTRAINT chk_conta_ativa CHECK (ativa IN (0,1)),
  CONSTRAINT uk_dados_bancarios UNIQUE (codigo_banco, agencia, numero_conta, tipo_conta),
  CONSTRAINT uk_cliente_tipo_conta UNIQUE (cliente_id, tipo_conta),
  CONSTRAINT uk_conta_cliente_id UNIQUE (cliente_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credenciais_administrativas (
  id CHAR(36) PRIMARY KEY,
  nome_exibicao VARCHAR(120) NOT NULL,
  usuario VARCHAR(60) NOT NULL,
  senha_hash VARCHAR(180) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT uk_credencial_admin_usuario UNIQUE (usuario),
  CONSTRAINT chk_credencial_admin_ativa CHECK (ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sessoes_administrativas (
  id CHAR(36) PRIMARY KEY,
  administrador_id CHAR(36) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  criada_em DATETIME(6) NOT NULL,
  expira_em DATETIME(6) NOT NULL,
  revogada_em DATETIME(6) NULL,
  CONSTRAINT fk_sessao_admin FOREIGN KEY (administrador_id)
    REFERENCES credenciais_administrativas(id),
  CONSTRAINT uk_sessao_admin_token UNIQUE (token_hash),
  CONSTRAINT chk_expiracao_sessao_admin CHECK (expira_em > criada_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE alteracoes_plano_cliente (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  administrador_id CHAR(36) NOT NULL,
  nome_administrador VARCHAR(120) NOT NULL,
  plano_anterior VARCHAR(20) NOT NULL,
  plano_novo VARCHAR(20) NOT NULL,
  motivo VARCHAR(240) NOT NULL,
  alterado_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_alteracao_plano_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_alteracao_plano_admin FOREIGN KEY (administrador_id)
    REFERENCES credenciais_administrativas(id),
  CONSTRAINT chk_alteracao_plano_anterior
    CHECK (plano_anterior IN ('STANDARD','PLUS')),
  CONSTRAINT chk_alteracao_plano_novo
    CHECK (plano_novo IN ('STANDARD','PLUS')),
  CONSTRAINT chk_alteracao_plano_diferente CHECK (plano_anterior <> plano_novo)
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
  CONSTRAINT uk_lancamento_referencia UNIQUE (referencia_id, tipo_referencia, conta_id, tipo_lancamento),
  CONSTRAINT chk_tipo_referencia CHECK (tipo_referencia IN ('TRANSFERENCIA','PIX','CARTAO','INVESTIMENTO')),
  CONSTRAINT chk_tipo_lancamento CHECK (tipo_lancamento IN ('TRANSFERENCIA_SAIDA','TRANSFERENCIA_ENTRADA','PIX_SAIDA','PIX_ENTRADA','TAXA')),
  CONSTRAINT chk_valor_lancamento CHECK (valor > 0),
  CONSTRAINT chk_natureza_lancamento CHECK (natureza IN ('DEBITO','CREDITO')),
  CONSTRAINT chk_saldo_apos_lancamento CHECK (saldo_apos >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE credenciais_acesso (
  cliente_id CHAR(36) PRIMARY KEY,
  usuario VARCHAR(60) NOT NULL,
  senha_hash VARCHAR(180) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  CONSTRAINT fk_credencial_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT uk_credencial_usuario UNIQUE (usuario),
  CONSTRAINT chk_credencial_ativa CHECK (ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sessoes_acesso (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  conta_id CHAR(36) NOT NULL,
  token_hash CHAR(64) NOT NULL,
  criada_em DATETIME(6) NOT NULL,
  expira_em DATETIME(6) NOT NULL,
  revogada_em DATETIME(6) NULL,
  CONSTRAINT fk_sessao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_sessao_conta_cliente FOREIGN KEY (cliente_id, conta_id)
    REFERENCES contas(cliente_id, id),
  CONSTRAINT uk_sessao_token UNIQUE (token_hash),
  CONSTRAINT chk_expiracao_sessao CHECK (expira_em > criada_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_instituicoes_ativas ON instituicoes_bancarias(ativa, nome);
CREATE INDEX idx_contas_cliente ON contas(cliente_id, ativa);
CREATE INDEX idx_sessoes_admin_expiracao
  ON sessoes_administrativas(administrador_id, expira_em);
CREATE INDEX idx_alteracoes_plano_cliente_data
  ON alteracoes_plano_cliente(cliente_id, alterado_em DESC);
CREATE INDEX idx_transferencias_pendentes ON transferencias(situacao, agendada_para);
CREATE INDEX idx_transferencias_origem_data ON transferencias(conta_origem_id, solicitada_em);
CREATE INDEX idx_lancamentos_conta_data ON lancamentos_conta(conta_id, ocorrido_em DESC);
CREATE INDEX idx_sessoes_cliente_expiracao ON sessoes_acesso(cliente_id, expira_em);

-- Crédito de cartão por cliente: aprovação somente após aceite; sem emissão/alocação nesta etapa.
CREATE TABLE credito_clientes (
  cliente_id CHAR(36) PRIMARY KEY,
  total_aprovado DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_credito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT chk_credito_aprovado CHECK (total_aprovado >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE solicitacoes_credito (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  total_solicitado DECIMAL(19,2) NOT NULL,
  motivo VARCHAR(240) NOT NULL,
  situacao VARCHAR(20) NOT NULL,
  total_ofertado DECIMAL(19,2) NULL,
  administrador_id CHAR(36) NULL,
  motivo_decisao VARCHAR(240) NULL,
  criada_em DATETIME(6) NOT NULL,
  decidida_em DATETIME(6) NULL,
  expira_em DATETIME(6) NULL,
  encerrada_em DATETIME(6) NULL,
  cliente_pendente CHAR(36) GENERATED ALWAYS AS
    (CASE WHEN situacao IN ('REQUESTED','OFFERED') THEN cliente_id ELSE NULL END) STORED,
  CONSTRAINT uk_credito_pendente UNIQUE (cliente_pendente),
  CONSTRAINT uk_solicitacao_cliente UNIQUE (cliente_id, id),
  CONSTRAINT fk_solicitacao_credito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT fk_solicitacao_credito_admin FOREIGN KEY (administrador_id) REFERENCES credenciais_administrativas(id),
  CONSTRAINT chk_solicitacao_total CHECK (total_solicitado > 0),
  CONSTRAINT chk_solicitacao_oferta CHECK (total_ofertado IS NULL OR (total_ofertado > 0 AND total_ofertado <= total_solicitado)),
  CONSTRAINT chk_solicitacao_situacao CHECK (situacao IN ('REQUESTED','OFFERED','ACCEPTED','REJECTED','CANCELLED','EXPIRED')),
  CONSTRAINT chk_oferta_prazo CHECK (situacao NOT IN ('OFFERED','ACCEPTED','EXPIRED')
    OR (total_ofertado IS NOT NULL AND administrador_id IS NOT NULL AND expira_em > decidida_em))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE operacoes_credito (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  solicitacao_id CHAR(36) NOT NULL,
  chave_idempotencia CHAR(36) NOT NULL,
  hash_requisicao CHAR(64) NOT NULL,
  resposta_json LONGTEXT NOT NULL,
  CONSTRAINT uk_operacao_credito_chave UNIQUE (cliente_id, chave_idempotencia),
  CONSTRAINT fk_operacao_solicitacao FOREIGN KEY (cliente_id, solicitacao_id)
    REFERENCES solicitacoes_credito(cliente_id, id),
  CONSTRAINT chk_operacao_credito_json CHECK (JSON_VALID(resposta_json)),
  CONSTRAINT chk_operacao_credito_hash CHECK (hash_requisicao REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_solicitacao_credito_cliente ON solicitacoes_credito(cliente_id, criada_em DESC);
CREATE INDEX idx_solicitacao_credito_analise ON solicitacoes_credito(situacao, criada_em);

-- Pix integrado ao schema inicial, conforme o guia.
-- Inclui destinatarios externos e favoritos externos.

CREATE TABLE chaves_pix (
  id CHAR(36) PRIMARY KEY,
  conta_id CHAR(36) NOT NULL,
  tipo_chave VARCHAR(20) NOT NULL,
  valor_normalizado VARCHAR(120) NOT NULL,
  ativa TINYINT(1) NOT NULL DEFAULT 1,
  valor_ativo VARCHAR(120)
    GENERATED ALWAYS AS (IF(ativa = 1, valor_normalizado, NULL)) STORED,
  criada_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_chave_pix_conta FOREIGN KEY (conta_id) REFERENCES contas(id),
  CONSTRAINT uk_chave_pix_valor_ativo UNIQUE (valor_ativo),
  CONSTRAINT chk_chave_pix_tipo CHECK (
    tipo_chave IN ('CPF_CNPJ','EMAIL','CELULAR','ALEATORIA')
  ),
  CONSTRAINT chk_chave_pix_ativa CHECK (ativa IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pix (
  id CHAR(36) PRIMARY KEY,
  conta_origem_id CHAR(36) NOT NULL,
  conta_destino_id CHAR(36) NULL,
  escopo_destino VARCHAR(20) NOT NULL DEFAULT 'INTERNO',
  tipo_chave VARCHAR(20) NOT NULL,
  chave_mascarada VARCHAR(120) NOT NULL,
  hash_chave CHAR(64) NOT NULL,
  nome_destinatario VARCHAR(120) NOT NULL,
  documento_destinatario_mascarado VARCHAR(24) NOT NULL,
  codigo_banco_destino VARCHAR(20) NOT NULL DEFAULT '90001',
  nome_banco_destino VARCHAR(120) NOT NULL DEFAULT 'SPBank',
  valor DECIMAL(19,2) NOT NULL,
  situacao VARCHAR(20) NOT NULL,
  chave_idempotencia CHAR(36) NOT NULL,
  hash_requisicao CHAR(64) NOT NULL,
  solicitada_em DATETIME(6) NOT NULL,
  agendada_para DATE NULL,
  processada_em DATETIME(6) NULL,
  referencia_liquidacao VARCHAR(120) NULL,
  codigo_falha VARCHAR(60) NULL,
  mensagem_falha VARCHAR(240) NULL,
  recorrencia_id CHAR(36) NULL,
  frequencia_recorrencia VARCHAR(20) NULL,
  numero_ocorrencia INT NULL,
  total_ocorrencias INT NULL,
  CONSTRAINT fk_pix_origem FOREIGN KEY (conta_origem_id) REFERENCES contas(id),
  CONSTRAINT fk_pix_destino FOREIGN KEY (conta_destino_id) REFERENCES contas(id),
  CONSTRAINT uk_pix_idempotencia UNIQUE (conta_origem_id, chave_idempotencia),
  CONSTRAINT chk_pix_tipo_chave CHECK (
    tipo_chave IN ('CPF_CNPJ','EMAIL','CELULAR','ALEATORIA')
  ),
  CONSTRAINT chk_pix_valor CHECK (valor > 0),
  CONSTRAINT chk_pix_hash_chave CHECK (hash_chave REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_pix_hash_requisicao CHECK (hash_requisicao REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_pix_situacao CHECK (
    situacao IN ('AGENDADO','PROCESSANDO','CONCLUIDO','FALHA','CANCELADO')
  ),
  CONSTRAINT chk_pix_recorrencia CHECK (
    (
      recorrencia_id IS NULL
      AND frequencia_recorrencia IS NULL
      AND numero_ocorrencia IS NULL
      AND total_ocorrencias IS NULL
    )
    OR
    (
      recorrencia_id IS NOT NULL
      AND frequencia_recorrencia IN ('SEMANAL','MENSAL')
      AND numero_ocorrencia >= 1
      AND total_ocorrencias BETWEEN 2 AND 24
      AND numero_ocorrencia <= total_ocorrencias
    )
  ),
  CONSTRAINT chk_pix_escopo_destino CHECK (
    (escopo_destino = 'INTERNO' AND conta_destino_id IS NOT NULL)
    OR
    (escopo_destino = 'EXTERNO' AND conta_destino_id IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favoritos_pix (
  id CHAR(36) PRIMARY KEY,
  conta_origem_id CHAR(36) NOT NULL,
  chave_pix_id CHAR(36) NULL,
  valor_chave_normalizado VARCHAR(120) NOT NULL,
  escopo_destino VARCHAR(20) NOT NULL DEFAULT 'INTERNO',
  nome_destinatario VARCHAR(120) NOT NULL,
  codigo_banco_destino VARCHAR(20) NOT NULL DEFAULT '90001',
  nome_banco_destino VARCHAR(120) NOT NULL DEFAULT 'SPBank',
  documento_destinatario_mascarado VARCHAR(24) NULL,
  chave_mascarada VARCHAR(120) NOT NULL,
  tipo_chave VARCHAR(20) NOT NULL,
  criado_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_favorito_pix_origem FOREIGN KEY (conta_origem_id) REFERENCES contas(id),
  CONSTRAINT fk_favorito_pix_chave FOREIGN KEY (chave_pix_id) REFERENCES chaves_pix(id),
  CONSTRAINT uk_favorito_pix_chave UNIQUE (
    conta_origem_id, tipo_chave, valor_chave_normalizado
  ),
  CONSTRAINT chk_favorito_pix_tipo CHECK (
    tipo_chave IN ('CPF_CNPJ','EMAIL','CELULAR','ALEATORIA')
  ),
  CONSTRAINT chk_favorito_pix_escopo CHECK (
    (escopo_destino = 'INTERNO' AND chave_pix_id IS NOT NULL)
    OR
    (escopo_destino = 'EXTERNO' AND chave_pix_id IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_chaves_pix_conta ON chaves_pix(conta_id, ativa, criada_em);
CREATE INDEX idx_pix_pendentes ON pix(situacao, agendada_para);
CREATE INDEX idx_pix_origem_data ON pix(conta_origem_id, solicitada_em);
CREATE INDEX idx_pix_recorrencia ON pix(conta_origem_id, recorrencia_id, situacao);
CREATE INDEX idx_favoritos_pix_origem ON favoritos_pix(conta_origem_id, criado_em);