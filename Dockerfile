# Stage 1: Build do backend
FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /app/backend

# Copia o pom.xml primeiro para cache de dependências
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código fonte e compila
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime do backend
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia o jar gerado
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Copia o arquivo .env
COPY backend/.env .

# Cria diretório para Flyway migrations
RUN mkdir -p /app/resources/db/migration

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]