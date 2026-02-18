# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependências
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Código fonte
COPY src ./src

# Build (sem testes dentro do Docker; testes rodam no CI)
RUN mvn -B -DskipTests clean package


# Estágio de Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o jar gerado
COPY --from=build /app/target/wallet-service-api.jar app.jar

# Usuário não-root por segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
