# Wallet Consumer

Microserviço responsável por consumir eventos de cadastro de clientes via Kafka, aplicar política de score e atualizar o estado do onboarding em MongoDB.

## Execução local

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

## Tópicos esperados

- `customer.registration.v1` (entrada)

## Observabilidade

- Actuator: `/actuator/health`
- Métricas: `/actuator/prometheus`
