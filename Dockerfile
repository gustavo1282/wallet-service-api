## Estágio de construção
#FROM eclipse-temurin:17-jdk-jammy AS builder
#WORKDIR /app
#COPY .mvn/ .mvn
#COPY mvnw pom.xml ./
#RUN ./mvnw dependency:go-offline
#COPY src ./src
#RUN ./mvnw clean package -DskipTests
#
## Estágio de execução (melhor prática com multi-stage)
#FROM eclipse-temurin:17-jre-jammy
#WORKDIR /app
#COPY --from=builder ./app/target/wallet-service-*.jar ./app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]

# ======================================================================
# STAGE 1: BUILD (Compilação e Geração do JAR)
# ----------------------------------------------------------------------
# Usamos a imagem Maven que já vem com o JDK 17 (Amazon Corretto) instalado.
FROM maven:3.9.5-amazoncorretto-17 AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia e baixa as dependências (otimiza o cache do Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Compila o projeto e gera o JAR executável na pasta target
RUN mvn package -DskipTests

# ======================================================================
# STAGE 2: PACKAGE (Runtime do Spring Boot)
# ----------------------------------------------------------------------
# Usamos uma imagem JRE 17 da Eclipse Temurin, que é ideal para runtime
# e muito mais leve que a imagem de build (apenas 130 MB).
FROM eclipse-temurin:17-jre-focal

# Porta que o Spring Boot usa
EXPOSE 8080

# Define o diretório de trabalho
WORKDIR /app

# Copia o JAR compilado da stage 'build' para esta stage leve
# O Spring Boot Maven Plugin cria o JAR executável com todas as dependências.
COPY --from=build /app/target/*.jar app.jar

# Comando para iniciar a aplicação Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]