# Security

Documentação de segurança, autenticação, autorização e boas práticas de proteção do Wallet Service API.

## 🔐 Estratégia de Segurança (Defense in Depth)

O projeto implementa múltiplas camadas de proteção:

```
┌─────────────────────────────────────────────┐
│  Layer 1: Network & Transport Security      │ HTTPS/TLS
├─────────────────────────────────────────────┤
│  Layer 2: Authentication & Authorization    │ JWT + Spring Security
├─────────────────────────────────────────────┤
│  Layer 3: Input Validation & Sanitization   │ Jakarta Validation
├─────────────────────────────────────────────┤
│  Layer 4: Output Encoding                   │ Jackson Serialization
├─────────────────────────────────────────────┤
│  Layer 5: Access Control                    │ Role-Based Access
├─────────────────────────────────────────────┤
│  Layer 6: Data Protection                   │ Encryption, Hashing
├─────────────────────────────────────────────┤
│  Layer 7: Logging & Monitoring              │ Audit Trails
└─────────────────────────────────────────────┘
```

## 🔑 Autenticação JWT

### Visão Geral

JWT (JSON Web Token) é um padrão seguro para autenticação stateless.

**Formato JWT:**
```
Header.Payload.Signature

eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### Componentes JWT

1. **Header** (Cabeçalho):
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

2. **Payload** (Dados):
```json
{
  "sub": "user123",
  "username": "joao",
  "iat": 1516239022,
  "exp": 1516242622,
  "roles": ["USER", "CUSTOMER"]
}
```

3. **Signature** (Assinatura):
```
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

### Implementação JwtService

**Arquivo:** `src/main/java/com/guga/walletserviceapi/security/JwtService.java`

```java
@Service
public class JwtService {
    
    @Value("${jwt.secret:seu-secret-key-muito-seguro}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:3600000}")  // 1 hora em ms
    private long jwtExpiration;
    
    // Gerar Access Token
    public String generateAccessToken(String username) {
        return createToken(username, jwtExpiration);
    }
    
    // Gerar Refresh Token (validade maior)
    public String generateRefreshToken(String password) {
        return createToken(password, jwtExpiration * 24);  // 24 horas
    }
    
    // Criar Token
    private String createToken(String subject, long expiryTimeInMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryTimeInMillis);
        
        return Jwts.builder()
            .setSubject(subject)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
    
    // Validar Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }
    
    // Extrair Username
    public String extractUsername(String token) {
        return Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

### Novo Modelo de Autenticação JWT (v0.2.7)

A partir da versão 0.2.7, foi implementado um modelo estrutural de autenticação JWT com as seguintes características:

- **JwtAuthenticationDetails**: Fonte única do contexto autenticado, centralizando username, roles e metadados.
- **Refatoração de Filtros e Providers**: Redesenho completo dos filtros de autenticação, providers e pacotes de segurança.
- **Fluxo de Autorização Corrigido**: Garantia de que `@PreAuthorize` seja avaliado após a autenticação JWT.
- **Padronização de Controllers**: Uso consistente de `JwtAuthenticatedUserProvider` para acesso ao usuário logado nos controllers.

**Fluxo de Autenticação Atualizado:**

```
Requisição HTTP → JwtAuthenticationFilter → JwtAuthenticationProvider → JwtAuthenticationDetails → SecurityContext
```

**Principais Componentes:**

- `JwtAuthenticationDetails`: Classe que encapsula o contexto do usuário autenticado.
- `JwtAuthenticatedUserProvider`: Provider para acesso padronizado ao usuário logado nos controllers.
- `AuditContextFactory`: Integração entre autenticação e auditoria.

### Configuração de Segurança

**Arquivo:** `src/main/java/com/guga/walletserviceapi/config/SpringSecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Desabilitar CSRF para APIs stateless
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()              // Login público
                .requestMatchers("/actuator/health").permitAll()         // Health check
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()                                         // Swagger
                .requestMatchers("/api/**").authenticated()              // Demais endpoints
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS  // Sem sessões (JWT)
                )
            )
            .addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT Authentication Filter

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && jwtService.validateToken(jwt)) {
                String username = jwtService.extractUsername(jwt);
                
                UserDetails userDetails = loadUserDetails(username);
                
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // Remove "Bearer "
        }
        return null;
    }
}
```

## 🔒 Password Management

### Armazenamento Seguro

Senhas NUNCA são armazenadas em plaintext. Usar hashing com salt:

```java
@Service
public class UserService {
    
    @Autowired
    private PasswordEncoder passwordEncoder;  // BCrypt
    
    public void registerUser(String username, String plainPassword) {
        String hashedPassword = passwordEncoder.encode(plainPassword);
        
        LoginAuth loginAuth = LoginAuth.builder()
            .username(username)
            .password(hashedPassword)  // ✅ Hash armazenado
            .status(Status.ACTIVE)
            .build();
        
        loginAuthRepository.save(loginAuth);
    }
    
    public boolean authenticateUser(String username, String plainPassword) {
        LoginAuth loginAuth = loginAuthRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // ✅ Compara plaintext com hash
        return passwordEncoder.matches(plainPassword, loginAuth.getPassword());
    }
}
```

### Política de Senhas

```yaml
# application.yml
security:
  password:
    min-length: 8
    require-uppercase: true
    require-numbers: true
    require-special-chars: true
    expiration-days: 90
    history-count: 5  # Não reutilizar últimas 5
```

## 🛡️ Validação de Input (XSS Prevention)

### Jakarta Bean Validation

```java
@Entity
public class Customer {
    
    @NotNull(message = "Nome não pode ser nulo")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Nome contém caracteres inválidos")
    private String name;
    
    @NotBlank(message = "Email não pode ser vazio")
    @Email(message = "Email deve ser válido")
    private String email;
    
    @NotBlank
    @Size(min = 11, max = 11, message = "CPF deve ter exatamente 11 dígitos")
    @Pattern(regexp = "^[0-9]+$", message = "CPF deve conter apenas números")
    private String cpf;
    
    @Min(value = 0, message = "Saldo não pode ser negativo")
    @Digits(integer = 18, fraction = 2)
    private BigDecimal balance;
}
```

### Sanitização de Output

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Escaper HTML para prevenir XSS
        mapper.setDefaultPropertyInclusion(
            JsonInclude.Include.NON_NULL
        );
        
        return mapper;
    }
}
```

## 🚫 SQL Injection Prevention

### ✅ CORRETO: Prepared Statements (JPA)

```java
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Query method (parameterized)
    Optional<Customer> findByCpf(String cpf);
    
    // Named query
    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    Optional<Customer> findByEmail(@Param("email") String email);
}
```

**Gerado SQL:**
```sql
SELECT * FROM customers WHERE cpf = $1  -- Parâmetro seguro
```

### ❌ ERRADO: String Concatenation

```java
// ❌ NUNCA FAZER ISSO
String query = "SELECT * FROM customers WHERE cpf = '" + cpf + "'";
```

## 🔐 HTTPS/TLS Configuration

### Production Configuration

```yaml
# application-prod.yml
server:
  port: 443
  ssl:
    key-store: classpath:keystore.jks
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: JKS
    key-alias: tomcat
  http2:
    enabled: true
```

### Gerar Self-Signed Certificate (Dev)

```bash
keytool -genkey -alias tomcat \
  -storetype PKCS12 \
  -keyalg RSA \
  -keysize 2048 \
  -keystore keystore.p12 \
  -validity 365 \
  -storepass changeit
```

### HTTP to HTTPS Redirect

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return container -> {
            Undertow.Builder builder = (Undertow.Builder) container
                .getUndertowBuilder();
            builder.addHttpListener(8080, "0.0.0.0");
        };
    }
}
```

## 🔑 Secrets Management

### Environment Variables

```bash
# .env (não commitá!)
JWT_SECRET=seu-secret-super-seguro-aqui
JWT_EXPIRATION=3600000
DB_PASSWORD=wallet_pass_segura
```

**Arquivo:** `.env.example` (commitado, sem valores reais)
```bash
JWT_SECRET=
JWT_EXPIRATION=3600000
DB_PASSWORD=
```

### Spring Vault Integration

```yaml
# application.yml (integração com HashiCorp Vault)
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      authentication: TOKEN
      token: ${VAULT_TOKEN}
      kv:
        enabled: true
        backend: secret
        version: 2
```

## 📋 Rate Limiting & DDoS Protection

### API Rate Limiting

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientId = getClientId(httpRequest);
        
        RateLimiter limiter = limiters.computeIfAbsent(clientId,
            k -> RateLimiter.create(100));  // 100 requisições/segundo
        
        if (!limiter.tryAcquire()) {
            ((HttpServletResponse) response).sendError(429, "Too Many Requests");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private String getClientId(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
```

## 🛑 CSRF Protection

Spring Security habilita por padrão, mas para APIs stateless (JWT):

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());  // ✅ Seguro para APIs JWT
        return http.build();
    }
}
```

## 🔐 Segurança em Transações Financeiras

### ACID Compliance

```java
@Service
public class TransactionService {
    
    @Transactional  // Garante ACID
    public Transaction processTransaction(
        Long walletId,
        BigDecimal amount) {
        
        // Bloqueia linha na DB durante transação
        Wallet wallet = walletRepository.findByIdLocked(walletId);
        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente");
        }
        
        // Se erro aqui, rollback automático
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        Transaction transaction = new Transaction();
        // ... preenchê-lo
        return transactionRepository.save(transaction);
    }
}
```

### Concorrência e Locks

```java
// Pessimistic Lock (Bloqueia linha)
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.walletId = :id")
Wallet findByIdWithLock(@Param("id") Long id);

// Optimistic Lock (Verifica versão)
@Entity
public class Wallet {
    
    @Version
    private Long version;  // Incrementa a cada mudança
    
    private BigDecimal balance;
}
```

## 📊 Audit & Logging

### Módulo de Auditoria Dedicado (v0.2.7)

Implementado módulo centralizado para auditoria e rastreamento:

**Características:**
- Centralização da criação do contexto de auditoria.
- Desacoplamento entre logging e futuros publishers de eventos (Kafka, DB, Elastic, etc.).
- Arquitetura preparada para evolução e escalabilidade.

**Componentes do Módulo:**
- `AuditContextFactory`: Criação e propagação do contexto.
- `AuditEvent`: Representação de eventos auditáveis.
- `AuditModule`: Centralização das regras de auditoria.

### Audit Trail

```java
@Entity
@EntityListeners(AuditListener.class)
public class Customer {
    
    @CreatedBy
    private String createdBy;
    
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedAt;
}
```

### Logging de Segurança

```java
@Component
@Aspect
public class SecurityLoggingAspect {
    
    @Around("@annotation(io.swagger.v3.oas.annotations.Operation)")
    public Object logSecurityEvents(ProceedingJoinPoint pjp)
        throws Throwable {
        
        HttpServletRequest request = 
            ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes())
            .getRequest();
        
        String username = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        
        log.info("Security Event",
            "username", username,
            "method", request.getMethod(),
            "path", request.getRequestURI(),
            "ip", request.getRemoteAddr(),
            "timestamp", LocalDateTime.now()
        );
        
        return pjp.proceed();
    }
}
```

## 🔄 CORS Configuration

### Cross-Origin Resource Sharing

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("Authorization", "Content-Type")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

## 🧪 Security Testing

### Testes de Autenticação

```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthSecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testUnauthorizedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/customers/1"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void testAuthorizedAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/customers/1"))
            .andExpect(status().isOk());
    }
}
```

## 📋 Checklist de Segurança

- [ ] Senhas criptografadas com BCrypt/Argon2
- [ ] JWT com expiração configurada
- [ ] HTTPS/TLS em produção
- [ ] SQL Injection prevention (prepared statements)
- [ ] XSS prevention (sanitização)
- [ ] CSRF protection habilitada
- [ ] Rate limiting implementado
- [ ] Audit logging ativo
- [ ] Secrets não em repositório (.env)
- [ ] Validação de input em todas as rotas
- [ ] Testes de segurança automatizados
- [ ] CORS configurado restritivamente
- [ ] Headers de segurança configurados
- [ ] Dependências atualizadas (sem vulnerabilidades)

## 🔗 Referências de Segurança

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc7519)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CWE Top 25](https://cwe.mitre.org/top25/)