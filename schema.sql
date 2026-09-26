CREATE DATABASE IF NOT EXISTS reclame
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE reclame;

SELECT DATABASE();

CREATE TABLE IF NOT EXISTS denuncias (
    id INT AUTO_INCREMENT PRIMARY KEY,

    protocolo VARCHAR(36) NOT NULL UNIQUE,

    modo ENUM('anonimo', 'identificado') NOT NULL,

    categoria VARCHAR(40) NOT NULL,

    dados_criptografados TEXT NOT NULL,

    email_denunciante_criptografado TEXT NULL,

    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_categoria (categoria),

    INDEX idx_criado_em (criado_em)

) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

SHOW TABLES;

DESCRIBE denuncias;
