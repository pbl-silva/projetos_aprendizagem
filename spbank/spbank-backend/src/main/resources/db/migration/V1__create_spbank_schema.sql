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
  CONSTRAINT chk_tipo_lancamento CHECK (tipo_lancamento IN ('TRANSFERENCIA_SAIDA','TRANSFERENCIA_ENTRADA','PIX_SAIDA','PIX_ENTRADA','TAXA','PAGAMENTO_CARTAO','INVESTIMENTO_APLICACAO')),
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
-- Distribuição por bandeira, conforme a seção 22.4 do guia.
CREATE TABLE linhas_credito_cartao (
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(20) NOT NULL,
  limite_alocado DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  valor_comprometido DECIMAL(19,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (cliente_id, bandeira),
  CONSTRAINT fk_linha_credito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT chk_linha_credito_bandeira CHECK (bandeira IN ('BRISA', 'MISTERSHARD', 'ELU')),
  CONSTRAINT chk_linha_credito_alocado CHECK (limite_alocado >= 0),
  CONSTRAINT chk_linha_credito_comprometido CHECK (valor_comprometido >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE operacoes_distribuicao_credito (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  chave_idempotencia CHAR(36) NOT NULL,
  hash_requisicao CHAR(64) NOT NULL,
  resposta_json LONGTEXT NOT NULL,
  CONSTRAINT uk_distribuicao_credito_chave UNIQUE (cliente_id, chave_idempotencia),
  CONSTRAINT fk_distribuicao_credito_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT chk_distribuicao_credito_json CHECK (JSON_VALID(resposta_json)),
  CONSTRAINT chk_distribuicao_credito_hash CHECK (hash_requisicao REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_linhas_credito_cliente ON linhas_credito_cartao(cliente_id);

-- Instrumentos de cart?o: se??o 22.4.6, RN-CAR-014 a RN-CAR-025.
CREATE TABLE cartoes (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(20) NOT NULL,
  cartao_anterior_id CHAR(36) NULL,
  nome_impresso VARCHAR(160) NOT NULL,
  final_numero CHAR(4) NOT NULL,
  hash_numero CHAR(64) NOT NULL,
  numero_cifrado VARCHAR(256) NOT NULL,
  cvv_cifrado VARCHAR(256) NOT NULL,
  hash_pin VARCHAR(256) NULL,
  situacao VARCHAR(24) NOT NULL,
  erros_pin INT NOT NULL DEFAULT 0,
  bloqueio_pin BOOLEAN NOT NULL DEFAULT FALSE,
  bloqueio_voluntario BOOLEAN NOT NULL DEFAULT FALSE,
  bloqueio_administrativo BOOLEAN NOT NULL DEFAULT FALSE,
  motivo_voluntario VARCHAR(240) NULL,
  motivo_administrativo VARCHAR(240) NULL,
  valida_ate DATE NOT NULL,
  criada_em DATETIME(6) NOT NULL,
  instrumento_vigente VARCHAR(64) GENERATED ALWAYS AS
    (CASE WHEN situacao IN ('PENDING_ACTIVATION','ACTIVE') THEN
      CONCAT(cliente_id, ':', bandeira, ':', CASE WHEN situacao = 'ACTIVE' THEN 'A' ELSE 'P' END)
      ELSE NULL END) STORED,
  CONSTRAINT uk_cartao_numero UNIQUE (hash_numero),
  CONSTRAINT uk_cartao_sucessor UNIQUE (cartao_anterior_id),
  CONSTRAINT uk_cartao_vigente UNIQUE (instrumento_vigente),
  CONSTRAINT fk_cartao_linha FOREIGN KEY (cliente_id, bandeira) REFERENCES linhas_credito_cartao(cliente_id, bandeira),
  CONSTRAINT fk_cartao_anterior FOREIGN KEY (cartao_anterior_id) REFERENCES cartoes(id),
  CONSTRAINT chk_cartao_situacao CHECK (situacao IN ('PENDING_ACTIVATION','ACTIVE','CANCELLED','REPLACED','EXPIRED')),
  CONSTRAINT chk_cartao_erros CHECK (erros_pin BETWEEN 0 AND 3),
  CONSTRAINT chk_cartao_pin CHECK (situacao <> 'ACTIVE' OR hash_pin IS NOT NULL),
  CONSTRAINT chk_cartao_pin_bloqueado CHECK (bloqueio_pin = (erros_pin = 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_cartoes_cliente ON cartoes(cliente_id, criada_em DESC);
CREATE INDEX idx_cartoes_ciclo ON cartoes(situacao, valida_ate, id);

CREATE TABLE auditoria_bloqueio_cartao (
  id CHAR(36) PRIMARY KEY,
  cartao_id CHAR(36) NOT NULL,
  responsavel_id CHAR(36) NOT NULL,
  acao VARCHAR(40) NOT NULL,
  motivo VARCHAR(240) NOT NULL,
  ocorrida_em DATETIME(6) NOT NULL,
  CONSTRAINT fk_auditoria_cartao FOREIGN KEY (cartao_id) REFERENCES cartoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_auditoria_cartao ON auditoria_bloqueio_cartao(cartao_id, ocorrida_em);

-- N?cleo financeiro de cart?o, RN-CAR-028 a RN-CAR-073.
CREATE TABLE configuracoes_fatura_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  due_day INT NOT NULL,
  pending_due_day INT NOT NULL,
  effective_reference VARCHAR(240),
  first_reference VARCHAR(240),
  credit_balance DECIMAL(19,2) NOT NULL,
  CONSTRAINT fk_configuracoes_fatura_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT uk_configuracao_cartao UNIQUE (cliente_id,bandeira)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_configuracoes_fatura_cartao_cliente ON configuracoes_fatura_cartao(cliente_id);

CREATE TABLE compras_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  card_id CHAR(36),
  merchant VARCHAR(240),
  principal DECIMAL(19,2) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  refunded_principal DECIMAL(19,2) NOT NULL,
  refunded_interest DECIMAL(19,2) NOT NULL,
  discounted_interest DECIMAL(19,2) NOT NULL,
  plan VARCHAR(240) NOT NULL,
  installments INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_compras_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_compras_cartao_cliente ON compras_cartao(cliente_id);

CREATE TABLE parcelas_compra_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  source_type VARCHAR(240),
  source_id CHAR(36),
  numero_parcela INT NOT NULL,
  referencia VARCHAR(240),
  nominal_due DATE NOT NULL,
  effective_due DATE NOT NULL,
  closing_at DATETIME(6) NOT NULL,
  principal DECIMAL(19,2) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  remaining_principal DECIMAL(19,2) NOT NULL,
  remaining_interest DECIMAL(19,2) NOT NULL,
  status VARCHAR(240),
  financed_agreement_id CHAR(36),
  CONSTRAINT fk_parcelas_compra_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_parcelas_compra_cartao_cliente ON parcelas_compra_cartao(cliente_id);

CREATE TABLE faturas_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  referencia VARCHAR(240),
  nominal_due DATE NOT NULL,
  effective_due DATE NOT NULL,
  closing_at DATETIME(6) NOT NULL,
  closed BOOLEAN NOT NULL,
  billed_total DECIMAL(19,2) NOT NULL,
  CONSTRAINT fk_faturas_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT uk_fatura_cartao_referencia UNIQUE (cliente_id,bandeira,referencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_faturas_cartao_cliente ON faturas_cartao(cliente_id);

CREATE TABLE itens_fatura_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  invoice_id CHAR(36),
  obligation_id CHAR(36),
  source_type VARCHAR(240),
  original_principal DECIMAL(19,2) NOT NULL,
  original_interest DECIMAL(19,2) NOT NULL,
  CONSTRAINT fk_itens_fatura_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_itens_fatura_cartao_cliente ON itens_fatura_cartao(cliente_id);

CREATE TABLE tentativas_compra_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240),
  card_id CHAR(36),
  last_four VARCHAR(240),
  merchant VARCHAR(240),
  amount DECIMAL(19,2) NOT NULL,
  status VARCHAR(240),
  reason VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_tentativas_compra_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_tentativas_compra_cartao_cliente ON tentativas_compra_cartao(cliente_id);

CREATE TABLE solicitacoes_estorno_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  purchase_id CHAR(36),
  principal DECIMAL(19,2) NOT NULL,
  reason VARCHAR(240),
  status VARCHAR(240),
  administrator_id CHAR(36),
  decision_reason VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_solicitacoes_estorno_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_solicitacoes_estorno_cartao_cliente ON solicitacoes_estorno_cartao(cliente_id);

CREATE TABLE estornos_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  request_id CHAR(36),
  purchase_id CHAR(36),
  principal DECIMAL(19,2) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  credit DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_estornos_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_estornos_cartao_cliente ON estornos_cartao(cliente_id);

CREATE TABLE pagamentos_fatura_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  purpose VARCHAR(240),
  proposal_id CHAR(36),
  method VARCHAR(240),
  amount DECIMAL(19,2) NOT NULL,
  applied_amount DECIMAL(19,2) NOT NULL,
  credited_amount DECIMAL(19,2) NOT NULL,
  discount DECIMAL(19,2) NOT NULL,
  status VARCHAR(240),
  account_id CHAR(36),
  administrator_id CHAR(36),
  reason VARCHAR(240),
  code VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  paid_at DATETIME(6),
  CONSTRAINT fk_pagamentos_fatura_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_pagamentos_fatura_cartao_cliente ON pagamentos_fatura_cartao(cliente_id);

CREATE TABLE alocacoes_pagamento_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  payment_id CHAR(36),
  obligation_id CHAR(36),
  kind VARCHAR(240),
  interest_paid DECIMAL(19,2) NOT NULL,
  principal_paid DECIMAL(19,2) NOT NULL,
  fine_paid DECIMAL(19,2) NOT NULL,
  mora_paid DECIMAL(19,2) NOT NULL,
  discount DECIMAL(19,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_alocacoes_pagamento_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_alocacoes_pagamento_cartao_cliente ON alocacoes_pagamento_cartao(cliente_id);

CREATE TABLE propostas_acordo_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  covered_ids LONGTEXT,
  previous_agreement_id CHAR(36),
  debt DECIMAL(19,2) NOT NULL,
  rate DECIMAL(27,8) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  entry DECIMAL(19,2) NOT NULL,
  total DECIMAL(19,2) NOT NULL,
  installments INT NOT NULL,
  plan VARCHAR(240) NOT NULL,
  valid_on DATE NOT NULL,
  status VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_propostas_acordo_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_propostas_acordo_cartao_cliente ON propostas_acordo_cartao(cliente_id);

CREATE TABLE acordos_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  proposal_id CHAR(36),
  previous_agreement_id CHAR(36),
  principal DECIMAL(19,2) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  rate DECIMAL(27,8) NOT NULL,
  installments INT NOT NULL,
  plan VARCHAR(240) NOT NULL,
  status VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_acordos_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_acordos_cartao_cliente ON acordos_cartao(cliente_id);

CREATE TABLE episodios_atraso_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  obligation_id CHAR(36),
  plan VARCHAR(240) NOT NULL,
  fine_rate DECIMAL(27,8) NOT NULL,
  mora_rate DECIMAL(27,8) NOT NULL,
  initial_base DECIMAL(19,2) NOT NULL,
  last_processed DATE NOT NULL,
  accrued_raw DECIMAL(27,8) NOT NULL,
  fine_charged DECIMAL(19,2) NOT NULL,
  mora_charged DECIMAL(19,2) NOT NULL,
  remaining_fine DECIMAL(19,2) NOT NULL,
  remaining_mora DECIMAL(19,2) NOT NULL,
  closed BOOLEAN NOT NULL,
  CONSTRAINT fk_episodios_atraso_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_episodios_atraso_cartao_cliente ON episodios_atraso_cartao(cliente_id);

CREATE TABLE alteracoes_vencimento_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  previous_day INT NOT NULL,
  new_day INT NOT NULL,
  effective_reference VARCHAR(240),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_alteracoes_vencimento_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_alteracoes_vencimento_cartao_cliente ON alteracoes_vencimento_cartao(cliente_id);

CREATE TABLE preferencias_pagamento_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  account_id CHAR(36),
  CONSTRAINT fk_preferencias_pagamento_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_preferencias_pagamento_cartao_cliente ON preferencias_pagamento_cartao(cliente_id);

CREATE TABLE operacoes_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  chave_idempotencia CHAR(36),
  fingerprint VARCHAR(240),
  response_json LONGTEXT,
  CONSTRAINT fk_operacoes_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
  CONSTRAINT uk_operacoes_cartao_chave UNIQUE (cliente_id,chave_idempotencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_operacoes_cartao_cliente ON operacoes_cartao(cliente_id);

CREATE TABLE parcelas_acordo_cartao (
  id CHAR(36) PRIMARY KEY,
  cliente_id CHAR(36) NOT NULL,
  bandeira VARCHAR(240) NOT NULL,
  source_type VARCHAR(240),
  source_id CHAR(36),
  numero_parcela INT NOT NULL,
  referencia VARCHAR(240),
  nominal_due DATE NOT NULL,
  effective_due DATE NOT NULL,
  closing_at DATETIME(6) NOT NULL,
  principal DECIMAL(19,2) NOT NULL,
  interest DECIMAL(19,2) NOT NULL,
  remaining_principal DECIMAL(19,2) NOT NULL,
  remaining_interest DECIMAL(19,2) NOT NULL,
  status VARCHAR(240),
  financed_agreement_id CHAR(36),
  CONSTRAINT fk_parcelas_acordo_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_parcelas_acordo_cartao_cliente ON parcelas_acordo_cartao(cliente_id);

-- Invariantes financeiras de cart?o.
ALTER TABLE configuracoes_fatura_cartao ADD CONSTRAINT ck_configuracoes_fatura_cartao_credit_balance CHECK (credit_balance >= 0);
ALTER TABLE compras_cartao ADD CONSTRAINT ck_compras_cartao_principal CHECK (principal >= 0);
ALTER TABLE compras_cartao ADD CONSTRAINT ck_compras_cartao_interest CHECK (interest >= 0);
ALTER TABLE compras_cartao ADD CONSTRAINT ck_compras_cartao_refunded_principal CHECK (refunded_principal >= 0);
ALTER TABLE compras_cartao ADD CONSTRAINT ck_compras_cartao_refunded_interest CHECK (refunded_interest >= 0);
ALTER TABLE compras_cartao ADD CONSTRAINT ck_compras_cartao_discounted_interest CHECK (discounted_interest >= 0);
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT ck_parcelas_compra_cartao_principal CHECK (principal >= 0);
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT ck_parcelas_compra_cartao_interest CHECK (interest >= 0);
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT ck_parcelas_compra_cartao_remaining_principal CHECK (remaining_principal >= 0);
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT ck_parcelas_compra_cartao_remaining_interest CHECK (remaining_interest >= 0);
ALTER TABLE faturas_cartao ADD CONSTRAINT ck_faturas_cartao_billed_total CHECK (billed_total >= 0);
ALTER TABLE itens_fatura_cartao ADD CONSTRAINT ck_itens_fatura_cartao_original_principal CHECK (original_principal >= 0);
ALTER TABLE itens_fatura_cartao ADD CONSTRAINT ck_itens_fatura_cartao_original_interest CHECK (original_interest >= 0);
ALTER TABLE tentativas_compra_cartao ADD CONSTRAINT ck_tentativas_compra_cartao_amount CHECK (amount >= 0);
ALTER TABLE solicitacoes_estorno_cartao ADD CONSTRAINT ck_solicitacoes_estorno_cartao_principal CHECK (principal >= 0);
ALTER TABLE estornos_cartao ADD CONSTRAINT ck_estornos_cartao_principal CHECK (principal >= 0);
ALTER TABLE estornos_cartao ADD CONSTRAINT ck_estornos_cartao_interest CHECK (interest >= 0);
ALTER TABLE estornos_cartao ADD CONSTRAINT ck_estornos_cartao_credit CHECK (credit >= 0);
ALTER TABLE pagamentos_fatura_cartao ADD CONSTRAINT ck_pagamentos_fatura_cartao_amount CHECK (amount >= 0);
ALTER TABLE pagamentos_fatura_cartao ADD CONSTRAINT ck_pagamentos_fatura_cartao_applied_amount CHECK (applied_amount >= 0);
ALTER TABLE pagamentos_fatura_cartao ADD CONSTRAINT ck_pagamentos_fatura_cartao_credited_amount CHECK (credited_amount >= 0);
ALTER TABLE pagamentos_fatura_cartao ADD CONSTRAINT ck_pagamentos_fatura_cartao_discount CHECK (discount >= 0);
ALTER TABLE alocacoes_pagamento_cartao ADD CONSTRAINT ck_alocacoes_pagamento_cartao_interest_paid CHECK (interest_paid >= 0);
ALTER TABLE alocacoes_pagamento_cartao ADD CONSTRAINT ck_alocacoes_pagamento_cartao_principal_paid CHECK (principal_paid >= 0);
ALTER TABLE alocacoes_pagamento_cartao ADD CONSTRAINT ck_alocacoes_pagamento_cartao_fine_paid CHECK (fine_paid >= 0);
ALTER TABLE alocacoes_pagamento_cartao ADD CONSTRAINT ck_alocacoes_pagamento_cartao_mora_paid CHECK (mora_paid >= 0);
ALTER TABLE alocacoes_pagamento_cartao ADD CONSTRAINT ck_alocacoes_pagamento_cartao_discount CHECK (discount >= 0);
ALTER TABLE propostas_acordo_cartao ADD CONSTRAINT ck_propostas_acordo_cartao_debt CHECK (debt >= 0);
ALTER TABLE propostas_acordo_cartao ADD CONSTRAINT ck_propostas_acordo_cartao_rate CHECK (rate >= 0);
ALTER TABLE propostas_acordo_cartao ADD CONSTRAINT ck_propostas_acordo_cartao_interest CHECK (interest >= 0);
ALTER TABLE propostas_acordo_cartao ADD CONSTRAINT ck_propostas_acordo_cartao_entry CHECK (entry >= 0);
ALTER TABLE propostas_acordo_cartao ADD CONSTRAINT ck_propostas_acordo_cartao_total CHECK (total >= 0);
ALTER TABLE acordos_cartao ADD CONSTRAINT ck_acordos_cartao_principal CHECK (principal >= 0);
ALTER TABLE acordos_cartao ADD CONSTRAINT ck_acordos_cartao_interest CHECK (interest >= 0);
ALTER TABLE acordos_cartao ADD CONSTRAINT ck_acordos_cartao_rate CHECK (rate >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_fine_rate CHECK (fine_rate >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_mora_rate CHECK (mora_rate >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_accrued_raw CHECK (accrued_raw >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_fine_charged CHECK (fine_charged >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_mora_charged CHECK (mora_charged >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_remaining_fine CHECK (remaining_fine >= 0);
ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_episodios_atraso_cartao_remaining_mora CHECK (remaining_mora >= 0);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT ck_parcelas_acordo_cartao_principal CHECK (principal >= 0);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT ck_parcelas_acordo_cartao_interest CHECK (interest >= 0);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT ck_parcelas_acordo_cartao_remaining_principal CHECK (remaining_principal >= 0);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT ck_parcelas_acordo_cartao_remaining_interest CHECK (remaining_interest >= 0);
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT uk_parcelas_compra_cartao_origem UNIQUE (source_id, numero_parcela);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT uk_parcelas_acordo_cartao_origem UNIQUE (source_id, numero_parcela);
ALTER TABLE itens_fatura_cartao ADD CONSTRAINT uk_item_cartao_origem UNIQUE (obligation_id);
ALTER TABLE operacoes_cartao ADD CONSTRAINT chk_operacao_cartao_json CHECK (JSON_VALID(response_json));
ALTER TABLE parcelas_compra_cartao ADD CONSTRAINT fk_parcelas_compra_cartao_source_id FOREIGN KEY (source_id) REFERENCES compras_cartao(id);
ALTER TABLE parcelas_acordo_cartao ADD CONSTRAINT fk_parcelas_acordo_cartao_source_id FOREIGN KEY (source_id) REFERENCES acordos_cartao(id);
ALTER TABLE itens_fatura_cartao ADD CONSTRAINT fk_itens_fatura_cartao_invoice_id FOREIGN KEY (invoice_id) REFERENCES faturas_cartao(id);
ALTER TABLE solicitacoes_estorno_cartao ADD CONSTRAINT fk_solicitacoes_estorno_cartao_purchase_id FOREIGN KEY (purchase_id) REFERENCES compras_cartao(id);
ALTER TABLE estornos_cartao ADD CONSTRAINT fk_estornos_cartao_request_id FOREIGN KEY (request_id) REFERENCES solicitacoes_estorno_cartao(id);
ALTER TABLE pagamentos_fatura_cartao ADD CONSTRAINT fk_pagamentos_fatura_cartao_proposal_id FOREIGN KEY (proposal_id) REFERENCES propostas_acordo_cartao(id);
ALTER TABLE acordos_cartao ADD CONSTRAINT fk_acordos_cartao_proposal_id FOREIGN KEY (proposal_id) REFERENCES propostas_acordo_cartao(id);
ALTER TABLE preferencias_pagamento_cartao ADD CONSTRAINT fk_preferencias_pagamento_cartao_account_id FOREIGN KEY (account_id) REFERENCES contas(id);

CREATE TABLE faturas_proposta_acordo_cartao (
id CHAR(36) PRIMARY KEY,
cliente_id CHAR(36) NOT NULL,
bandeira VARCHAR(240) NOT NULL,
proposal_id CHAR(36) NOT NULL,
invoice_id CHAR(36) NOT NULL,
CONSTRAINT fk_faturas_proposta_acordo_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_faturas_proposta_acordo_cartao_cliente ON faturas_proposta_acordo_cartao(cliente_id);

CREATE TABLE alocacoes_encargo_atraso_cartao (
id CHAR(36) PRIMARY KEY,
cliente_id CHAR(36) NOT NULL,
bandeira VARCHAR(240) NOT NULL,
payment_id CHAR(36) NOT NULL,
obligation_id CHAR(36) NOT NULL,
fine DECIMAL(19,2) NOT NULL,
mora DECIMAL(19,2) NOT NULL,
created_at DATETIME(6) NOT NULL,
CONSTRAINT fk_alocacoes_encargo_atraso_cartao_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_alocacoes_encargo_atraso_cartao_cliente ON alocacoes_encargo_atraso_cartao(cliente_id);

ALTER TABLE episodios_atraso_cartao ADD CONSTRAINT ck_atraso_base CHECK (initial_base >= 0);
ALTER TABLE faturas_proposta_acordo_cartao ADD CONSTRAINT fk_fatura_proposta_proposta FOREIGN KEY (proposal_id) REFERENCES propostas_acordo_cartao(id);
ALTER TABLE faturas_proposta_acordo_cartao ADD CONSTRAINT fk_fatura_proposta_fatura FOREIGN KEY (invoice_id) REFERENCES faturas_cartao(id);

-- Investment checkpoints INV-1/INV-2 and pending applications.
ALTER TABLE contas ADD UNIQUE KEY uk_conta_titular_investimento (id,cliente_id);

CREATE TABLE produtos_investimento (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(240) NOT NULL,
  type VARCHAR(240) NOT NULL,
  minimum_application DECIMAL(19,2) NOT NULL,
  minimum_remaining DECIMAL(19,2) NOT NULL,
  risk VARCHAR(240) NOT NULL,
  active BIT(1) NOT NULL,
  application_days INT NULL,
  redemption_days INT NULL,
  payment_days INT NULL,
  cdi_percent DECIMAL(19,8) NULL,
  term_days INT NULL,
  liquidity VARCHAR(240) NULL,
  CHECK (type IN ('FUND','CDB')),
  CHECK (minimum_application>0 AND minimum_remaining>=0),
  CHECK ((type='FUND' AND application_days IS NOT NULL AND redemption_days IS NOT NULL AND payment_days IS NOT NULL AND application_days=0 AND redemption_days=0 AND payment_days=1 AND cdi_percent IS NULL AND term_days IS NULL AND liquidity IS NULL) OR (type='CDB' AND application_days IS NULL AND redemption_days IS NULL AND payment_days IS NULL AND cdi_percent IS NOT NULL AND term_days IS NOT NULL AND liquidity IS NOT NULL AND cdi_percent>0 AND term_days>0 AND liquidity='AT_MATURITY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE cotas_investimento (
  id CHAR(36) PRIMARY KEY,
  product_id CHAR(36) NOT NULL,
  quote_date DATE NOT NULL,
  price DECIMAL(27,12) NOT NULL,
  UNIQUE (product_id,quote_date),
  CHECK (price>0),
  FOREIGN KEY (product_id) REFERENCES produtos_investimento(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE feriados_investimento (
  id CHAR(36) PRIMARY KEY,
  month_day VARCHAR(240) NULL,
  exact_date DATE NULL,
  UNIQUE (month_day),
  UNIQUE (exact_date),
  CHECK ((month_day IS NOT NULL AND exact_date IS NULL) OR (month_day IS NULL AND exact_date IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE aplicacoes_investimento (
  id CHAR(36) PRIMARY KEY,
  customer_id CHAR(36) NOT NULL,
  account_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  effective_date DATE NOT NULL,
  status VARCHAR(240) NOT NULL,
  requested_at DATETIME(6) NOT NULL,
  processed_at DATETIME(6) NULL,
  position_id CHAR(36) NULL,
  balance_after DECIMAL(19,2) NULL,
  failure_code VARCHAR(240) NULL,
  FOREIGN KEY (customer_id) REFERENCES clientes(id),
  FOREIGN KEY (account_id,customer_id) REFERENCES contas(id,cliente_id),
  FOREIGN KEY (product_id) REFERENCES produtos_investimento(id),
  CHECK (status IN ('PENDING','COMPLETED','FAILED')),
  CHECK (amount>0),
  CHECK ((status='PENDING' AND processed_at IS NULL AND position_id IS NULL AND balance_after IS NULL AND failure_code IS NULL) OR (status='COMPLETED' AND processed_at IS NOT NULL AND position_id IS NOT NULL AND balance_after>=0 AND failure_code IS NULL) OR (status='FAILED' AND processed_at IS NOT NULL AND position_id IS NULL AND balance_after IS NULL AND failure_code IS NOT NULL)),
  INDEX idx_investment_due (status,effective_date,requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE posicoes_investimento (
  id CHAR(36) PRIMARY KEY,
  application_id CHAR(36) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  account_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  type VARCHAR(240) NOT NULL,
  terms_json LONGTEXT NOT NULL,
  principal DECIMAL(19,2) NOT NULL,
  effective_date DATE NOT NULL,
  units DECIMAL(27,12) NULL,
  unit_price DECIMAL(27,12) NULL,
  quote_date DATE NULL,
  maturity_date DATE NULL,
  UNIQUE (application_id),
  FOREIGN KEY (application_id) REFERENCES aplicacoes_investimento(id),
  FOREIGN KEY (customer_id) REFERENCES clientes(id),
  FOREIGN KEY (account_id,customer_id) REFERENCES contas(id,cliente_id),
  FOREIGN KEY (product_id) REFERENCES produtos_investimento(id),
  CHECK (principal>0),
  CHECK (JSON_VALID(terms_json)),
  CHECK ((type='FUND' AND units IS NOT NULL AND unit_price IS NOT NULL AND units>0 AND unit_price>0 AND quote_date IS NOT NULL AND maturity_date IS NULL) OR (type='CDB' AND units IS NULL AND unit_price IS NULL AND quote_date IS NULL AND maturity_date IS NOT NULL)),
  INDEX idx_investment_positions_customer (customer_id,effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE operacoes_investimento (
  id CHAR(36) PRIMARY KEY,
  customer_id CHAR(36) NOT NULL,
  operation_key CHAR(36) NOT NULL,
  fingerprint CHAR(64) NOT NULL,
  response_json LONGTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE (customer_id,operation_key),
  FOREIGN KEY (customer_id) REFERENCES clientes(id),
  CHECK (fingerprint REGEXP '^[0-9a-f]{64}$'),
  CHECK (JSON_VALID(response_json))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- INV-4: internal versioned daily CDI factors, in a 252-business-day convention.
CREATE TABLE fatores_cdi_investimento (
  id CHAR(36) PRIMARY KEY,
  valid_from DATE NOT NULL,
  valid_through DATE NOT NULL,
  daily_factor DECIMAL(27,18) NOT NULL,
  business_days_basis INT NOT NULL,
  source_version VARCHAR(80) NOT NULL,
  CONSTRAINT uk_cdi_source_version UNIQUE (source_version),
  CONSTRAINT chk_cdi_dates CHECK (valid_through>=valid_from),
  CONSTRAINT chk_cdi_factor CHECK (daily_factor>0),
  CONSTRAINT chk_cdi_basis CHECK (business_days_basis=252),
  CONSTRAINT chk_cdi_version CHECK (CHAR_LENGTH(TRIM(source_version))>0),
  INDEX idx_cdi_validity (valid_from,valid_through)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE posicoes_investimento ADD UNIQUE KEY uk_posicao_titular_valoracao (id,customer_id);
CREATE TABLE avaliacoes_investimento (
  id CHAR(36) PRIMARY KEY,
  position_id CHAR(36) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  reference_date DATE NOT NULL,
  status VARCHAR(30) NOT NULL,
  gross_value DECIMAL(19,2) NULL,
  gross_earnings DECIMAL(19,2) NULL,
  calculated_through DATE NULL,
  calculation_fingerprint CHAR(64) NOT NULL,
  algorithm_version VARCHAR(30) NOT NULL,
  response_json LONGTEXT NOT NULL,
  CONSTRAINT fk_valuation_position_owner FOREIGN KEY (position_id,customer_id) REFERENCES posicoes_investimento(id,customer_id),
  CONSTRAINT uk_valuation_input UNIQUE (position_id,reference_date,calculation_fingerprint),
  CONSTRAINT chk_valuation_status CHECK (status IN ('AVAILABLE','DATA_UNAVAILABLE')),
  CONSTRAINT chk_valuation_amounts CHECK ((status='AVAILABLE' AND gross_value IS NOT NULL AND gross_value>=0 AND gross_earnings IS NOT NULL AND calculated_through IS NOT NULL AND calculated_through<=reference_date) OR (status='DATA_UNAVAILABLE' AND gross_value IS NULL AND gross_earnings IS NULL AND calculated_through IS NULL)),
  CONSTRAINT chk_valuation_fingerprint CHECK (calculation_fingerprint REGEXP '^[0-9a-f]{64}$'),
  CONSTRAINT chk_valuation_response CHECK (JSON_VALID(response_json)),
  INDEX idx_valuation_owner_date (customer_id,reference_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
