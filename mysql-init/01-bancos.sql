CREATE DATABASE IF NOT EXISTS objetos_db;
CREATE DATABASE IF NOT EXISTS solicitacoes_db;
CREATE USER IF NOT EXISTS 'objetos'@'%' IDENTIFIED BY 'objetos_local';
CREATE USER IF NOT EXISTS 'solicitacoes'@'%' IDENTIFIED BY 'solicitacoes_local';
GRANT ALL PRIVILEGES ON objetos_db.* TO 'objetos'@'%';
GRANT ALL PRIVILEGES ON solicitacoes_db.* TO 'solicitacoes'@'%';
