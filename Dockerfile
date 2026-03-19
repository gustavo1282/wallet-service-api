# syntax=docker/dockerfile:1.6

# ===============================
# STAGE 1 - BUILD
# ===============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 1) Copia só o descriptor primeiro (melhor cache por layer)
COPY pom.xml ./

# 2) Aquece/cacheia dependências (usa cache persistente do BuildKit)
#RUN --mount=type=cache,target=/root/.m2 \
##mvn -B -DskipTests dependency:go-offline
## -->  ~/.m2 #LINUX
## -->  %USERPROFILE%\.m2 #WINDOWS

RUN --mount=type=bind,source=~/.m2,target=/root/.m2 \
    mvn -B -o -DskipTests dependency:go-offline

    # 3) Agora copia o código
COPY src ./src

# 4) Build de verdade (usa o MESMO cache .m2)
#RUN --mount=type=cache,target=/root/.m2 \
RUN --mount=type=bind,source=~/.m2,target=/root/.m2 \
    mvn -B -DskipTests package

# ===============================
# STAGE 2 - RUNTIME
# ===============================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Mantém o jar com versão (do seu finalName) dentro do container
COPY --from=builder /build/target/*.jar /app/

# Cria um alias estável pra rodar sempre (opcional, mas recomendo)
RUN set -eux; \
    JAR="$(ls -1 /app/*.jar | head -n 1)"; \
    ln -sf "$JAR" /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]