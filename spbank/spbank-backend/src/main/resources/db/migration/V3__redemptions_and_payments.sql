ALTER TABLE lancamentos_conta DROP CHECK chk_tipo_referencia, DROP CHECK chk_tipo_lancamento;
ALTER TABLE lancamentos_conta ADD CONSTRAINT chk_tipo_referencia CHECK (tipo_referencia IN ('TRANSFERENCIA','PIX','CARTAO','INVESTIMENTO','PAGAMENTO')),
 ADD CONSTRAINT chk_tipo_lancamento CHECK (tipo_lancamento IN ('TRANSFERENCIA_SAIDA','TRANSFERENCIA_ENTRADA','PIX_SAIDA','PIX_ENTRADA','TAXA','PAGAMENTO_CARTAO','INVESTIMENTO_APLICACAO','INVESTIMENTO_RESGATE','PAGAMENTO_SAIDA','RECARGA_CELULAR_SAIDA'));
CREATE TABLE resgates_investimento (
 id CHAR(36) PRIMARY KEY, cliente_id CHAR(36) NOT NULL, dados JSON NOT NULL,
 INDEX ix_resgates_cliente(cliente_id)
);
CREATE TABLE catalogo_cobrancas_estudo(codigo_hash CHAR(64) PRIMARY KEY,dados JSON NOT NULL);
CREATE TABLE catalogo_recargas_celular(id CHAR(36) PRIMARY KEY,dados JSON NOT NULL);
CREATE TABLE pagamentos(id CHAR(36) PRIMARY KEY,cliente_id CHAR(36) NOT NULL,chave CHAR(36) NOT NULL,tipo VARCHAR(20) NOT NULL,alvo_hash CHAR(64) NOT NULL,situacao VARCHAR(20) NOT NULL,agendado_para DATE NOT NULL,criado_em TIMESTAMP(6) NOT NULL,dados JSON NOT NULL,
 UNIQUE KEY uk_pagamento_chave(cliente_id,chave),INDEX ix_pagamento_fila(situacao,agendado_para),INDEX ix_pagamento_cliente(cliente_id),
 CONSTRAINT chk_pagamento_status CHECK(situacao IN ('SCHEDULED','PROCESSING','COMPLETED','FAILED','CANCELLED')));

ALTER TABLE resgates_investimento
 ADD chave CHAR(36) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(dados,'$.key'))) STORED,
 ADD posicao_id CHAR(36) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(dados,'$.positionId'))) STORED,
 ADD posicao_pendente CHAR(36) GENERATED ALWAYS AS (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(dados,'$.status')) IN ('PENDING','CONVERSION_PENDING') THEN JSON_UNQUOTE(JSON_EXTRACT(dados,'$.positionId')) ELSE NULL END) STORED,
 ADD UNIQUE KEY uk_resgate_chave(cliente_id,chave),
 ADD UNIQUE KEY uk_resgate_pendente(posicao_pendente),
 ADD CONSTRAINT fk_resgate_cliente FOREIGN KEY(cliente_id) REFERENCES clientes(id),
 ADD CONSTRAINT fk_resgate_posicao FOREIGN KEY(posicao_id) REFERENCES posicoes_investimento(id);
ALTER TABLE pagamentos
 ADD cobranca_ativa CHAR(64) GENERATED ALWAYS AS (CASE WHEN tipo='BILL' AND situacao IN ('SCHEDULED','PROCESSING','COMPLETED') THEN alvo_hash ELSE NULL END) STORED,
 ADD UNIQUE KEY uk_pagamento_cobranca(cliente_id,cobranca_ativa),
 ADD CONSTRAINT fk_pagamento_cliente FOREIGN KEY(cliente_id) REFERENCES clientes(id);
