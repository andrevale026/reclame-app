# RECLAME — versão Java

Migração do backend Python/Flask para Java 21 + Spring Boot, mantendo o frontend HTML/CSS/JavaScript e o banco MySQL.

## Requisitos
- Java 21
- Maven 3.9+
- MySQL 8

## Configuração
Defina as variáveis de ambiente descritas em `.env.example`. O Spring Boot não carrega `.env` automaticamente; configure-as no sistema operacional/IDE/serviço de implantação.

A `ENCRYPTION_KEY` aceita a mesma chave Fernet usada na versão Python. Assim, os novos registros continuam no mesmo formato criptográfico.

## Executar
```bash
mvn spring-boot:run
```
Abra `http://localhost:5000`.

## Banco
A tabela existente `denuncias` é compatível. O arquivo `schema.sql` fica disponível para criação manual do banco.

## O que foi removido da versão Python
`.venv`, `.idea`, `__pycache__`, `requirements.txt`, `app.py`, `config.yml`, scripts `.bat`, `logo_old.png` e o SQL duplicado. Nenhum segredo do `.env` original foi copiado.
