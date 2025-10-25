#!/bin/bash

# Este script executa a análise do SonarQube localmente,
# replicando as configurações do perfil 'desenv' do sonar.yml.

# Pré-requisito: O container do SonarQube deve estar rodando na porta 9000
# (por exemplo: docker-compose up sonarqube)

export SONAR_LOCAL_TOKEN="wallet-service-api-sonar-token"

echo "Iniciando a análise do SonarQube para o perfil 'desenv'..."

# O comando completo do Maven/Sonar, pegando as configurações
# de host, login e profile do ambiente local (desenv)
#  -Dsonar.token=${SONAR_LOCAL_TOKEN} \
./mvnw clean verify sonar:sonar -DskipTests \
  -Dsonar.login=admin \
  -Dsonar.password=admin123 \
  -Dsonar.host.url=http://localhost:9000 \
  -P sonar

# Verifica o código de saída do comando Maven
if [ $? -eq 0 ]; then
    echo "Análise do SonarQube concluída com sucesso!"
    echo "Acesse o dashboard em: http://localhost:9000"
else
    echo "ERRO: A análise do SonarQube falhou. Verifique o log acima."
    echo "Certifique-se de que o SonarQube está ativo em http://localhost:9000"
fi
