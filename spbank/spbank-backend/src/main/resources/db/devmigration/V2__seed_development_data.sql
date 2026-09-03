INSERT INTO instituicoes_bancarias (
  codigo_estudo,
  nome,
  interna,
  ativa
) VALUES
  ('90001', 'SPBank', 1, 1),
  ('90002', 'Banco Aurora', 0, 1),
  ('90003', 'Banco Horizonte', 0, 1),
  ('90004', 'Banco Nexo', 0, 1),
  ('90005', 'Banco Solaris', 0, 1);

INSERT INTO clientes (
  id,
  nome_completo,
  cpf,
  data_nascimento,
  celular,
  email,
  cep,
  logradouro,
  numero_endereco,
  complemento,
  bairro,
  cidade,
  uf,
  ativo
) VALUES
  (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Alexandre Souza',
    '52998224725',
    '1990-05-15',
    '11987654321',
    'alexandre@spbank.local',
    '01001000',
    'Praça da Sé',
    '100',
    NULL,
    'Sé',
    'São Paulo',
    'SP',
    1
  ),
  (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'Roberta Silva',
    '11144477735',
    '1992-09-10',
    '21991234567',
    'roberta@spbank.local',
    '20040002',
    'Rua da Assembleia',
    '200',
    'Apto 301',
    'Centro',
    'Rio de Janeiro',
    'RJ',
    1
  );

INSERT INTO contas (
  id,
  cliente_id,
  nome_titular,
  documento_titular,
  codigo_banco,
  agencia,
  numero_conta,
  tipo_conta,
  plano_conta,
  saldo,
  ativa
) VALUES
  (
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Alexandre Souza',
    '52998224725',
    '90001',
    '0001',
    '100010',
    'CORRENTE',
    'PLUS',
    5000.00,
    1
  ),
  (
    '22222222-2222-2222-2222-222222222222',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'Roberta Silva',
    '11144477735',
    '90001',
    '0001',
    '200020',
    'CORRENTE',
    'STANDARD',
    1000.00,
    1
  );

INSERT INTO credenciais_acesso (
  cliente_id,
  usuario,
  senha_hash,
  ativa
) VALUES
  (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'alexandre',
    'pbkdf2_sha256$120000$48zsuVaGGsX1ozR1uw9AlA==$XMKEJGUIL0L6OHA3ZF+PIcsrcAcXy/pD/SBUgbrnVyY=',
    1
  ),
  (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'roberta',
    'pbkdf2_sha256$120000$0AsNBjkkKPXlsD32nJ6Dyw==$bBejGeR8cADsFZHARNfqpTZv2/tv7mFtv8vpWa61H/M=',
    1
  );

  INSERT INTO credenciais_administrativas (
  id,
  nome_exibicao,
  usuario,
  senha_hash,
  ativa
) VALUES (
  'cccccccc-cccc-cccc-cccc-cccccccccccc',
  'Gerente SPBank',
  'gerente',
  'pbkdf2_sha256$120000$/2pRwqqLAmVJkHdO45ByvA==$K9aXI/0uRATy1Jc5YONPD9a3Y07R+weul7Lsj7Xv4n8=',
  1
);

INSERT INTO alteracoes_plano_conta (
  id,
  conta_id,
  administrador_id,
  nome_administrador,
  plano_anterior,
  plano_novo,
  motivo,
  alterado_em
) VALUES (
  'dddddddd-dddd-dddd-dddd-dddddddddddd',
  '11111111-1111-1111-1111-111111111111',
  'cccccccc-cccc-cccc-cccc-cccccccccccc',
  'Gerente SPBank',
  'STANDARD',
  'PLUS',
  'Conta de demonstração aprovada para o plano PLUS',
  '2026-09-01 09:00:00.000000'
);