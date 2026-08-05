# syntax=docker/dockerfile:1

# ============================================
# STAGE 1: build - compila o projeto com Maven
# ============================================
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copia primeiro só o pom.xml para aproveitar cache de dependências
# (só baixa tudo de novo se o pom.xml mudar, não a cada alteração de código)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Agora copia o código-fonte e builda o jar (pulando os testes, que já
# rodam separadamente no CI/antes do build da imagem)
COPY src src
RUN ./mvnw clean package -DskipTests -Djacoco.skip=true -B

# ============================================
# STAGE 2: runtime - imagem final, só com o JRE
# ============================================
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# curl é usado apenas pelo healthcheck do docker-compose; a imagem jre-jammy
# não vem com ele por padrão
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root por segurança
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build /app/target/ecommerce-api-*.jar app.jar

RUN mkdir -p /app/logs && chown -R spring:spring /app
USER spring

EXPOSE 8080

# -Djava.awt.headless=true reforça o modo headless (a aplicação já detecta
# isso sozinha, mas é uma segurança a mais explícita)
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]