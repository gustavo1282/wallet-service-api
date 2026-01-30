#!/bin/bash

# Este script executa a análise do SonarQube localmente,
# replicando as configurações do perfil 'desenv' do sonar.yml.

# Pré-requisito: O container do SonarQube deve estar rodando na porta 9000
# (por exemplo: docker-compose up sonarqube)

echo "-- Iniciando a análise do SonarQube --"

export SONAR_LOCAL_TOKEN="squ_ea51cedf0f969507efcbb2a7ce1b985455004a61"
export PROFILE="test"

echo "-- Variaveis inicializadas --"
echo "-- PROFILE: ${PROFILE}"
echo "-- SONAR_LOCAL_TOKEN: *** *** *** ---"
echo "-- "
# O comando completo do Maven/Sonar, pegando as configurações
# de host, login e profile do ambiente local (desenv)
#  #-Dsonar.login=admin \
#  #-Dsonar.password=sonar \
echo "-- "
echo "-- maven: clean > verify > jacoco > profile"
./mvnw clean verify jacoco:report -Dspring.profiles.active=${PROFILE}

echo "-- "
echo "-- maven: sonar > skip tests > sonar [projectKey, url, token, coverage]"
echo "-- "

./mvnw sonar:sonar -DskipTests=true \
  -Dsonar.projectKey=com.gugawallet:wallet-service-api \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=${SONAR_LOCAL_TOKEN} \
  -Dsonar.junit.reportPaths=target/surefire-reports \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

echo "-- "
echo "-- "

# Verifica o código de saída do comando Maven
if [ $? -eq 0 ]; then
    echo "Análise do SonarQube concluída com sucesso!"
    echo "Acesse o dashboard em: http://localhost:9000"
else
    echo "ERRO: A análise do SonarQube falhou. Verifique o log acima."
    echo "Certifique-se de que o SonarQube está ativo em http://localhost:9000"
fi
