ALTER TABLE pix
  MODIFY COLUMN conta_destino_id CHAR(36) NULL,

  ADD COLUMN escopo_destino VARCHAR(20) NOT NULL DEFAULT 'INTERNO'
    AFTER conta_destino_id,

  ADD COLUMN codigo_banco_destino VARCHAR(20) NOT NULL DEFAULT '90001'
    AFTER documento_destinatario_mascarado,

  ADD COLUMN nome_banco_destino VARCHAR(120) NOT NULL DEFAULT 'SPBank'
    AFTER codigo_banco_destino,

  ADD COLUMN referencia_liquidacao VARCHAR(120) NULL
    AFTER processada_em,

  ADD CONSTRAINT chk_pix_escopo_destino CHECK (
    (
      escopo_destino = 'INTERNO'
      AND conta_destino_id IS NOT NULL
    )
    OR
    (
      escopo_destino = 'EXTERNO'
      AND conta_destino_id IS NULL
    )
  );


ALTER TABLE favoritos_pix
  DROP INDEX uk_favorito_pix,

  MODIFY COLUMN chave_pix_id CHAR(36) NULL,

  ADD COLUMN valor_chave_normalizado VARCHAR(120) NULL
    AFTER chave_pix_id,

  ADD COLUMN escopo_destino VARCHAR(20) NOT NULL DEFAULT 'INTERNO'
    AFTER valor_chave_normalizado,

  ADD COLUMN codigo_banco_destino VARCHAR(20) NOT NULL DEFAULT '90001'
    AFTER nome_destinatario,

  ADD COLUMN nome_banco_destino VARCHAR(120) NOT NULL DEFAULT 'SPBank'
    AFTER codigo_banco_destino,

  ADD COLUMN documento_destinatario_mascarado VARCHAR(24) NULL
    AFTER nome_banco_destino;


UPDATE favoritos_pix f
JOIN chaves_pix c
  ON c.id = f.chave_pix_id

SET f.valor_chave_normalizado =
  c.valor_normalizado;


ALTER TABLE favoritos_pix
  MODIFY COLUMN valor_chave_normalizado VARCHAR(120) NOT NULL,

  ADD CONSTRAINT uk_favorito_pix_chave
    UNIQUE (
      conta_origem_id,
      tipo_chave,
      valor_chave_normalizado
    ),

  ADD CONSTRAINT chk_favorito_pix_escopo CHECK (
    (
      escopo_destino = 'INTERNO'
      AND chave_pix_id IS NOT NULL
    )
    OR
    (
      escopo_destino = 'EXTERNO'
      AND chave_pix_id IS NULL
    )
  );