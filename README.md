# Arcana

Plataforma para mesas de RPG: usuários autenticados criam personagens, mestres criam e administram mesas, e jogadores conversam e rolam dados em tempo real.

## Tecnologias

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 17, Spring Boot 4.1, Spring Security (JWT com Auth0 java-jwt), Spring Data JPA, Spring WebSocket (STOMP), Bean Validation, Actuator, Lombok, Jackson |
| Banco de dados | PostgreSQL 16 com migrations Flyway |
| Frontend | React 18, TypeScript, Vite, React Router |
| Testes | JUnit / Spring Boot Test, Testcontainers (PostgreSQL), Spring Security Test |
| Infraestrutura | Docker (multi-stage), Docker Compose, Nginx, Caddy, GitHub Actions |

## Estrutura do projeto

```text
backend/src/main/java/com/arcana/backend/
├─ character/   personagens e NPCs
├─ chat/        WebSocket da mesa
├─ config/      segurança, CORS, WebSocket, Jackson
├─ table/       mesas e participantes
└─ user/        usuários e autenticação
backend/src/main/resources/db/migration/   migrations Flyway (V{n}__descricao.sql)
frontend/src/
├─ pages/       telas (um diretório por página, com .tsx e .css)
├─ services/    clientes HTTP e WebSocket
├─ context/     estado global de sessão
└─ types/       tipos compartilhados
```

O backend é organizado por funcionalidade. Cada módulo segue `controller/`, `service/`, `repository/`, `model/` e DTOs.

## Pré-requisitos

- Docker e Docker Compose
- Java 17 e Maven (para desenvolvimento local do backend)
- Node.js 20 e npm (para desenvolvimento local do frontend)

## Variáveis de ambiente

Copie `.env.example` para `.env` e preencha os valores. As variáveis principais são:

| Variável | Descrição |
| --- | --- |
| `DB_USER` | Usuário do PostgreSQL |
| `DB_PASSWORD` | Senha do PostgreSQL |
| `DB_HOST` | Host do banco (`postgres` dentro do Docker) |
| `JWT_SECRET` | Chave de assinatura dos tokens JWT |
| `ALLOWED_ORIGINS` | Origens permitidas no CORS (separadas por vírgula) |
| `DOMAIN` | Domínio público usado em produção (Caddy/Traefik) |

## Como rodar

### Com Docker (recomendado)

Sobe PostgreSQL, backend e frontend (Nginx na porta 4200) em containers:

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Health check: http://localhost:8080/actuator/health

### Desenvolvimento local

Backend (roda na porta 8080):

```bash
cd backend
./mvnw spring-boot:run
```

Frontend (roda na porta 5173 com Vite):

```bash
cd frontend
npm install
npm run dev
```

### Testes e build

```bash
cd backend && ./mvnw test
cd backend && ./mvnw verify
cd frontend && npm run build
```

## Arquitetura de containers

| Serviço | Imagem | Função |
| --- | --- | --- |
| `postgres` | `postgres:16-alpine` | Banco de dados com volume persistente e healthcheck |
| `backend` | `eclipse-temurin:17-jre-alpine` (build com Maven) | API Spring Boot na porta 8080, usuário não-root |
| `frontend` | `nginx:alpine` (build com Node 20) | Servidor estático do React na porta 4200, com reverse proxy para `/api` e `/ws` |

O `nginx.conf` do frontend faz proxy de `/api/` para o backend (HTTP) e de `/ws/` para o WebSocket, além de servir a SPA com fallback para `index.html`, gzip, cache de arquivos estáticos e cabeçalhos de segurança (CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy).

## Deploy

### Pipeline de CI/CD (GitHub Actions)

Definido em `.github/workflows/ci-cd.yml`, executado em todo push para `main`:

1. **Backend** — `./mvnw verify` (testes + build)
2. **Frontend** — `npm ci` + `npm run build`
3. **Deploy** — se os dois passos anteriores passarem, conecta por SSH na VPS e executa `scripts/deploy.sh`

O deploy usa os secrets do GitHub: `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_KNOWN_HOSTS`, `VPS_APP_PATH`, `VPS_COMPOSE_PROJECT`.

### Script de deploy (`scripts/deploy.sh`)

Executado na VPS via SSH:

1. Faz `git fetch` e `git reset --hard` para o SHA do commit
2. Roda `docker compose -f docker-compose.hostinger.yml build` e `up -d`
3. Aguarda os serviços (`postgres`, `backend`, `frontend`) ficarem saudáveis
4. Se algum serviço não ficar saudável, faz rollback automático para a versão anterior

### Arquivos de compose

| Arquivo | Ambiente | Diferencial |
| --- | --- | --- |
| `docker-compose.yml` | Desenvolvimento local | PostgreSQL + backend + frontend na rede `arcana-network` |
| `docker-compose.prod.yml` | Produção genérica | Sobrepõe o compose local adicionando o Caddy como reverse proxy com HTTPS automático (portas 80/443) |
| `docker-compose.hostinger.yml` | Produção na VPS Hostinger | Rede interna isolada, labels do Traefik para TLS com Let's Encrypt, sem portas expostas diretamente |

### Reverse proxy em produção

- **Caddy** (`docker-compose.prod.yml` + `Caddyfile`): termina TLS com certificados automáticos, aplica HSTS e faz proxy para o frontend na porta 4200.
- **Traefik** (`docker-compose.hostinger.yml`): roteia o tráfego pelo domínio configurado em `${DOMAIN}`, com TLS via certresolver `letsencrypt`.

Em ambos os casos, o tráfego chega ao frontend (Nginx), que por sua vez faz proxy de `/api` e `/ws` para o backend na rede interna.
