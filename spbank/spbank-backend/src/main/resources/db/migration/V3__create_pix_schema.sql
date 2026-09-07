ALTER TABLE lancamentos_conta
  DROP CHECK chk_tipo_lancamento;


ALTER TABLE lancamentos_conta
  ADD CONSTRAINT chk_tipo_lancamento CHECK (
    tipo_lancamento IN (
      'TRANSFERENCIA_SAIDA',
      'TRANSFERENCIA_ENTRADA',
      'PIX_SAIDA',
      'PIX_ENTRADA',
      'TAXA'
    )
  );


CREATE TABLE chaves_pix (
  id CHAR(36) PRIMARY KEY,

  conta_id CHAR(36) NOT NULL,

  tipo_chave VARCHAR(20) NOT NULL,

  valor_normalizado VARCHAR(120) NOT NULL,

  ativa TINYINT(1) NOT NULL DEFAULT 1,

  valor_ativo VARCHAR(120)
    GENERATED ALWAYS AS (
      IF(
        ativa = 1,
        valor_normalizado,
        NULL
      )
    ) STORED,

  criada_em DATETIME(6) NOT NULL,

  CONSTRAINT fk_chave_pix_conta
    FOREIGN KEY (conta_id)
    REFERENCES contas(id),

  CONSTRAINT uk_chave_pix_valor_ativo
    UNIQUE (valor_ativo),

  CONSTRAINT chk_chave_pix_tipo CHECK (
    tipo_chave IN (
      'CPF_CNPJ',
      'EMAIL',
      'CELULAR',
      'ALEATORIA'
    )
  ),

  CONSTRAINT chk_chave_pix_ativa CHECK (
    ativa IN (0, 1)
  )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE pix (
  id CHAR(36) PRIMARY KEY,

  conta_origem_id CHAR(36) NOT NULL,

  conta_destino_id CHAR(36) NOT NULL,

  tipo_chave VARCHAR(20) NOT NULL,

  chave_mascarada VARCHAR(120) NOT NULL,

  hash_chave CHAR(64) NOT NULL,

  nome_destinatario VARCHAR(120) NOT NULL,

  documento_destinatario_mascarado VARCHAR(24) NOT NULL,

  valor DECIMAL(19,2) NOT NULL,

  situacao VARCHAR(20) NOT NULL,

  chave_idempotencia CHAR(36) NOT NULL,

  hash_requisicao CHAR(64) NOT NULL,

  solicitada_em DATETIME(6) NOT NULL,

  agendada_para DATE NULL,

  processada_em DATETIME(6) NULL,

  codigo_falha VARCHAR(60) NULL,

  mensagem_falha VARCHAR(240) NULL,

  recorrencia_id CHAR(36) NULL,

  frequencia_recorrencia VARCHAR(20) NULL,

  numero_ocorrencia INT NULL,

  total_ocorrencias INT NULL,

  CONSTRAINT fk_pix_origem
    FOREIGN KEY (conta_origem_id)
    REFERENCES contas(id),

  CONSTRAINT fk_pix_destino
    FOREIGN KEY (conta_destino_id)
    REFERENCES contas(id),

  CONSTRAINT uk_pix_idempotencia
    UNIQUE (
      conta_origem_id,
      chave_idempotencia
    ),

  CONSTRAINT chk_pix_tipo_chave CHECK (
    tipo_chave IN (
      'CPF_CNPJ',
      'EMAIL',
      'CELULAR',
      'ALEATORIA'
    )
  ),

  CONSTRAINT chk_pix_valor CHECK (
    valor > 0
  ),

  CONSTRAINT chk_pix_hash_chave CHECK (
    hash_chave REGEXP '^[0-9a-f]{64}$'
  ),

  CONSTRAINT chk_pix_hash_requisicao CHECK (
    hash_requisicao REGEXP '^[0-9a-f]{64}$'
  ),

  CONSTRAINT chk_pix_situacao CHECK (
    situacao IN (
      'AGENDADO',
      'PROCESSANDO',
      'CONCLUIDO',
      'FALHA',
      'CANCELADO'
    )
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
      AND frequencia_recorrencia IN (
        'SEMANAL',
        'MENSAL'
      )
      AND numero_ocorrencia >= 1
      AND total_ocorrencias BETWEEN 2 AND 24
      AND numero_ocorrencia <= total_ocorrencias
    )

  )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE favoritos_pix (
  id CHAR(36) PRIMARY KEY,

  conta_origem_id CHAR(36) NOT NULL,

  chave_pix_id CHAR(36) NOT NULL,

  nome_destinatario VARCHAR(120) NOT NULL,

  chave_mascarada VARCHAR(120) NOT NULL,

  tipo_chave VARCHAR(20) NOT NULL,

  criado_em DATETIME(6) NOT NULL,

  CONSTRAINT fk_favorito_pix_origem
    FOREIGN KEY (conta_origem_id)
    REFERENCES contas(id),

  CONSTRAINT fk_favorito_pix_chave
    FOREIGN KEY (chave_pix_id)
    REFERENCES chaves_pix(id),

  CONSTRAINT uk_favorito_pix
    UNIQUE (
      conta_origem_id,
      chave_pix_id
    ),

  CONSTRAINT chk_favorito_pix_tipo CHECK (
    tipo_chave IN (
      'CPF_CNPJ',
      'EMAIL',
      'CELULAR',
      'ALEATORIA'
    )
  )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


CREATE INDEX idx_chaves_pix_conta
  ON chaves_pix (
    conta_id,
    ativa,
    criada_em
  );


CREATE INDEX idx_pix_pendentes
  ON pix (
    situacao,
    agendada_para
  );


CREATE INDEX idx_pix_origem_data
  ON pix (
    conta_origem_id,
    solicitada_em
  );


CREATE INDEX idx_pix_recorrencia
  ON pix (
    conta_origem_id,
    recorrencia_id,
    situacao
  );


CREATE INDEX idx_favoritos_pix_origem
  ON favoritos_pix (
    conta_origem_id,
    criado_em
  );