# syntax=docker/dockerfile:1

# ===== Estágio 1: build =====
# Usa o JDK completo só para compilar; essa camada não vai pra imagem final.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro só o wrapper e o pom.xml para aproveitar o cache de camadas
# do Docker: se as dependências não mudarem, essa etapa não roda de novo.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Agora copia o código-fonte e builda o jar (pula os testes aqui — os
# testes devem rodar no CI antes de chegar a este ponto, não no build da imagem).
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ===== Estágio 2: runtime =====
# Imagem final: só o JRE (bem menor que o JDK) + o jar já compilado.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Roda como usuário não-root por segurança (evita rodar a aplicação como root).
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
