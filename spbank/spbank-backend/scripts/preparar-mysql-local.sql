-- Executar uma vez com um administrador do MySQL local.
-- Nao altera credenciais de usuarios existentes nem apaga dados.
CREATE DATABASE IF NOT EXISTS spbank CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'spbank'@'localhost' IDENTIFIED BY 'spbank123';
GRANT ALL PRIVILEGES ON spbank.* TO 'spbank'@'localhost';
