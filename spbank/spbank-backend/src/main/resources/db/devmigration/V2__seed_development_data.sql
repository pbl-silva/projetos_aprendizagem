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
    'pbkdf2_sha256$120000$48zsuVaGGsX1ozR1uw9AlA==$0ykQKRLAO7puCJTN8zI1/T8sd6PY2xNRRZ86kCCek9o=',
    1
  ),
  (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'roberta',
    'pbkdf2_sha256$120000$0AsNBjkkKPXlsD32nJ6Dyw==$XQxMf9wvSi5HKbu5sPQOe31x73+GJUhu/urCvA9pBKM=',
    1
  );