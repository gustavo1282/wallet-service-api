# Observability

Documentação sobre logs, métricas, health checks e monitoramento do Wallet Service API.

## 🔍 Pilares da Observabilidade

```
┌──────────────────────────────────────────┐
│     Observabilidade em 3 Pilares        │
├──────────────────────────────────────────┤
│  1. Logs      - O que aconteceu?        │
│  2. Métricas  - Como está o sistema?    │
│  3. Traces    - Qual foi o caminho?     │
└──────────────────────────────────────────┘
```

## � Stack de Observabilidade Containerizada

A partir da versão 0.2.4, a observabilidade é totalmente containerizada via Docker Compose, incluindo:

- **Prometheus** (porta 9090): Coleta métricas da aplicação e OTel Collector.
- **Grafana** (porta 3000): Dashboards visuais para métricas e traces.
- **Jaeger** (porta 16686): Visualização de traces distribuídos.
- **OpenTelemetry Collector** (portas 4317/4318/8889): Recebe traces via OTLP, gera métricas spanmetrics e exporta para Jaeger/Prometheus.

**Arquivo:** `docker-compose.yml`

**Subir a stack:**
```bash
docker-compose up -d prometheus grafana jaeger otel-collector
```

**Acessar:**
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Jaeger: http://localhost:16686
- OTel Collector métricas: http://localhost:8889/metrics

## �📝 Logs

### Configuração de Logging

**Arquivo:** `src/main/resources/application-local.yml`

```yaml
logging:
  level:
    # Root logger
    root: INFO
    
    # Componentes específicos
    com.guga.walletserviceapi: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
    org.hibernate.engine.jdbc.batch: WARN
    
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/wallet-service.log
    max-size: 10MB
    max-history: 7
```

### Níveis de Log

| Nível | Uso | Exemplo |
|-------|-----|---------|
| **TRACE** | Informação muito detalhada | `TRACE: Entrando no método getCustomerById` |
| **DEBUG** | Informação para debug | `DEBUG: CPF validado com sucesso` |
| **INFO** | Eventos importantes | `INFO: Customer #1 criado com sucesso` |
| **WARN** | Possíveis problemas | `WARN: Saldo baixo na carteira #5` |
| **ERROR** | Erros que afetam função | `ERROR: CPF duplicado ao criar customer` |

### Padrões de Logging

#### Service Layer

```java
@Service
@Slf4j
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public Customer saveCustomer(Customer customer) {
        log.info("Iniciando criação de novo cliente",
            Map.of("email", customer.getEmail(),
                   "cpf", maskCpf(customer.getCpf())));
        
        try {
            validateCustomer(customer);
            Customer saved = customerRepository.save(customer);
            
            log.info("Cliente criado com sucesso",
                Map.of("customerId", saved.getCustomerId(),
                       "email", customer.getEmail()));
            
            return saved;
            
        } catch (DataIntegrityViolationException e) {
            log.error("Erro ao criar cliente - Email ou CPF duplicado",
                Map.of("email", customer.getEmail()),
                e);
            throw new ResourceBadRequestException("Email ou CPF já existe");
        }
    }
    
    private String maskCpf(String cpf) {
        return cpf.substring(0, 3) + "****" + cpf.substring(7);
    }
}
```

#### Controller Layer

```java
@RestController
@RequestMapping("/api/customers")
@Slf4j
public class CustomerController {
    
    @PostMapping("/customer")
    public ResponseEntity<Customer> createCustomer(
        @RequestBody Customer customer) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Nova requisição POST /customers",
            Map.of("correlationId", correlationId,
                   "clientIp", request.getRemoteAddr()));
        
        try {
            Customer created = customerService.saveCustomer(customer);
            
            log.info("Customer criado com sucesso",
                Map.of("correlationId", correlationId,
                       "customerId", created.getCustomerId()));
            
            return ResponseEntity.created(location).body(created);
            
        } catch (Exception e) {
            log.error("Falha ao criar customer",
                Map.of("correlationId", correlationId,
                       "error", e.getMessage()),
                e);
            throw e;
        }
    }
}
```

#### Log Estruturado (JSON)

```yaml
# application-prod.yml
logging:
  level:
    root: INFO
  format: json
```

**Exemplo de saída:**
```json
{
  "timestamp": "2024-12-08T10:30:45.123Z",
  "level": "INFO",
  "message": "Customer criado com sucesso",
  "logger_name": "com.guga.walletserviceapi.service.CustomerService",
  "customerId": 1,
  "email": "joao@example.com",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Acessar Logs

```bash
# Logs em tempo real
tail -f logs/wallet-service.log

# Buscar por erro específico
grep "ERROR" logs/wallet-service.log

# Buscar por customer ID
grep "customerId=1" logs/wallet-service.log

# Últimas 50 linhas
tail -50 logs/wallet-service.log
```

## 📊 Métricas (Prometheus)

### Configuração

**Dependência em `pom.xml`:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Arquivo:** `src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
    metrics:
      enabled: true
    health:
      show-details: always
```

### Métricas Disponíveis

#### Métricas de JVM

```
# Memória
jvm_memory_used_bytes             # Memória usada
jvm_memory_max_bytes              # Máximo disponível
jvm_memory_committed_bytes        # Alocado pelo SO

# Threads
jvm_threads_live                  # Threads ativas
jvm_threads_peak                  # Pico de threads

# Coleta de lixo
jvm_gc_memory_allocated_bytes     # Total alocado
jvm_gc_memory_promoted_bytes      # Promovido para old gen
```

#### Métricas de Traces (Spanmetrics)

```
traces_span_metrics_calls_total              # Total de spans por serviço/span
traces_span_metrics_duration_milliseconds_count  # Contagem de durações de spans
traces_span_metrics_duration_milliseconds_sum    # Soma das durações de spans
```

#### Métricas de HTTP

```
http_requests_total               # Total de requisições
http_request_duration_seconds     # Duração das requisições
http_server_requests_seconds      # Latência por endpoint
```

#### Métricas de Banco de Dados

```
db_connections_active             # Conexões ativas
db_connections_idle               # Conexões ociosas
db_connections_max                # Máximo de conexões
```

### Métricas Customizadas

```java
@Service
@Slf4j
public class TransactionService {
    
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public TransactionService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public Transaction processDeposit(Long walletId, BigDecimal amount) {
        
        // Counter: Total de depósitos
        meterRegistry.counter("transactions.deposit.total").increment();
        
        // Timer: Tempo de processamento
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // ... lógica de negócio
            
            // Gauge: Valor médio de depósito
            meterRegistry.gauge("transactions.deposit.amount",
                amount.doubleValue());
            
            sample.stop(Timer.builder("transactions.deposit.duration")
                .description("Tempo para processar depósito")
                .register(meterRegistry));
            
        } catch (Exception e) {
            meterRegistry.counter("transactions.deposit.failed")
                .increment();
            throw e;
        }
    }
}
```

### Acessar Prometheus

```bash
# Endpoint de métricas
curl http://localhost:8080/wallet-service-api/actuator/prometheus

# Exemplo de saída
jvm_memory_used_bytes{area="heap"} 356147200
jvm_threads_live 42
http_requests_total{method="POST",status="201"} 15
```

### Configurar Prometheus (Local)

**Arquivo:** `prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  scrape_timeout: 10s

scrape_configs:
  - job_name: 'wallet-service-api'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/wallet-service-api/actuator/prometheus'
```

**Executar Prometheus:**
```bash
docker run -d \
  -p 9090:9090 \
  -v $(pwd)/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

**Acessar Dashboard:** http://localhost:9090

## 🏥 Health Checks

### Configuração

```yaml
management:
  endpoint:
    health:
      show-details: always
      show-components: true
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

### Probes do Kubernetes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

### Endpoint Health

**GET** `/actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "hello": 1
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1048576000,
        "free": 524288000,
        "threshold": 10485760
      }
    },
    "livenessState": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

### Health Check Customizado

```java
@Component
public class WalletHealthIndicator extends AbstractHealthIndicator {
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        
        try {
            long countWallets = walletRepository.count();
            long countActiveWallets = walletRepository
                .countByStatus(Status.ACTIVE);
            
            builder.up()
                .withDetail("totalWallets", countWallets)
                .withDetail("activeWallets", countActiveWallets);
                
        } catch (Exception e) {
            builder.down()
                .withDetail("error", e.getMessage());
        }
    }
}
```

Acesso: `/actuator/health/walletHealth`

## 🔗 Tracing Distribuído (OpenTelemetry + Jaeger)

### Configuração com OTel Collector

**Dependências em `pom.xml`:**
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**Arquivo:** `src/main/resources/application-local.yml`
```yaml
management:
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
  tracing:
    enabled: true
    sampling:
      probability: 1.0
```

**Arquivo:** `otel-collector-config.yml`
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

connectors:
  spanmetrics:
    namespace: traces_span_metrics
    metrics_flush_interval: 15s
    dimensions:
      - name: http.method
      - name: http.route
      - name: http.status_code

exporters:
  prometheus:
    endpoint: 0.0.0.0:8889
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [spanmetrics, otlp/jaeger]
    metrics:
      receivers: [spanmetrics]
      processors: [batch]
      exporters: [prometheus]
```

**Métricas de Traces Geradas:**
- `traces_span_metrics_calls_total`: Contagem de spans.
- `traces_span_metrics_duration_milliseconds_count`: Duração de spans.
- Labels: `service_name`, `span_name`, `span_kind`, `http_method`, etc.

### Jaeger UI

Acessar: http://localhost:16686

## 📈 Alertas

### Prometheus Alerts

**Arquivo:** `prometheus/alerts.yml`

```yaml
groups:
  - name: wallet_service
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "Taxa de erro alta em {{ $labels.instance }}"
          description: "{{ $value }}% das requisições retornaram 5xx"
      
      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 1m
        annotations:
          summary: "Banco de dados indisponível"
          description: "PostgreSQL não está respondendo"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        annotations:
          summary: "Uso de memória crítico"
          description: "Memória utilizada: {{ humanizePercentage $value }}"
```

## 🔄 Correlação de Eventos

### Correlation ID

Implementar propagação de correlation ID em toda stack:

```java
@Component
@Slf4j
public class CorrelationIdFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader("X-Correlation-ID");
        
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // MDC = Mapped Diagnostic Context (SLF4J)
        MDC.put("correlationId", correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

**Padrão de log:**
```
[correlationId=550e8400-e29b-41d4-a716-446655440000] INFO: Customer criado
[correlationId=550e8400-e29b-41d4-a716-446655440000] INFO: Wallet criada
[correlationId=550e8400-e29b-41d4-a716-446655440000] INFO: Transação registrada
```

## 📊 Dashboards

### Grafana com Dashboards OpenTelemetry

Visualizar métricas Prometheus em tempo real.

Importe os dashboards oficiais via ID no Grafana (http://localhost:3000 > Dashboards > Import):

- **18860 - HTTP Metrics OpenTelemetry**: Métricas HTTP (requisições, latência).
- **19419 - OpenTelemetry APM**: APM completo (throughput, erro rate, traces).
- **15983 - OpenTelemetry Collector**: Saúde do OTel Collector.

**Configuração:**
- Data Source: Prometheus (http://prometheus:9090).
- Para dashboard 19419: Adicione variável template "span_kind" (Query: `label_values(traces_span_metrics_calls_total, span_kind)`) para filtrar dinamicamente por tipo de span (ex.: SPAN_KIND_INTERNAL).

**Executar Grafana:**
```bash
docker-compose up -d grafana
```

**Acessar:** http://localhost:3000 (admin/admin)

## 📋 Checklist de Observabilidade

- [ ] Logs estruturados implementados
- [ ] Métricas customizadas adicionadas
- [ ] Health checks configurados
- [ ] Correlation IDs propagados
- [ ] Prometheus rodando
- [ ] Alertas configurados
- [ ] Dashboards criados
- [ ] Retenção de logs definida
- [ ] Planos de disaster recovery
- [ ] OTel Collector configurado e rodando
- [ ] Dashboards OpenTelemetry importados (18860, 19419, 15983)
- [ ] Métricas spanmetrics coletadas no Prometheus

## 🔗 Referências

- [Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
- [Micrometer Prometheus](https://micrometer.io/docs/registry/prometheus)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry](https://opentelemetry.io/)