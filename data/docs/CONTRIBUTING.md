# Contributing

Guia para contribuição ao projeto Wallet Service API.

## 🎯 Código de Conduta

Somos comprometidos com um ambiente acolhedor e inclusivo. Esperamos que todos os contribuidores:

- Sejam respeitosos com colegas
- Aceitem críticas construtivas
- Focando no que é melhor para a comunidade
- Demonstrem empatia com outros membros

## 🚀 Como Começar

### 1. Fork do Repositório

```bash
# Ir para https://github.com/gustavo1282/wallet-service-api
# Clicar em "Fork" (canto superior direito)
```

### 2. Clonar o Repositório Local

```bash
git clone https://github.com/SEU_USUARIO/wallet-service-api.git
cd wallet-service-api

# Adicionar upstream (repositório original)
git remote add upstream https://github.com/gustavo1282/wallet-service-api.git
```

### 3. Criar Branch Feature

```bash
# Atualizar branch main
git fetch upstream
git checkout main
git merge upstream/main

# Criar nova branch
git checkout -b feature/meu-recurso
# Ou para bug fix:
git checkout -b bugfix/meu-bug
```

**Convenção de nomes:**
- Feature: `feature/descricao-curta`
- Bugfix: `bugfix/descricao-curta`
- Hotfix: `hotfix/descricao-urgente`
- Docs: `docs/atualizacao-documentacao`
- Test: `test/adição-testes`

## 💻 Desenvolvimento

### Setup do Ambiente

#### Pré-requisitos

```bash
# Verificar versões
java -version          # Deve ser 21+
mvn --version          # Deve ser 3.9.11+
git --version          # Qualquer versão recente
```

#### Configuração Inicial

```bash
# Instalar dependências
./mvnw clean install

# Executar em desenvolvimento
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Compilar sem rodar
./mvnw clean compile

# Rodar testes
./mvnw test
```

### Padrões de Código

#### Estilo de Código

Seguir Google Java Style Guide:
- Indentação: 4 espaços
- Line length: máximo 120 caracteres
- Quebra de linhas: antes de operadores

**Exemplo:**

```java
// ✅ BOM
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public Customer saveCustomer(Customer customer) {
        validateCustomer(customer);
        return customerRepository.save(customer);
    }
}

// ❌ RUIM
public class CustomerService{
private final CustomerRepository customerRepository;
public Customer saveCustomer(Customer customer){
validateCustomer(customer);
return customerRepository.save(customer);
}
}
```

#### Nomeação

| Tipo | Convenção | Exemplo |
|------|-----------|---------|
| Classes | PascalCase | `CustomerService` |
| Métodos | camelCase | `getCustomerById` |
| Constantes | UPPER_CASE | `MAX_WALLET_BALANCE` |
| Variáveis | camelCase | `walletBalance` |
| Packages | lowercase | `com.guga.walletserviceapi` |

#### Javadoc

Documentar classes públicas e métodos:

```java
/**
 * Serviço responsável por operações de clientes.
 * 
 * Fornece funcionalidades para:
 * - Criar novo cliente
 * - Atualizar informações
 * - Listar clientes com filtros
 * 
 * @author Gustavo
 * @version 1.0
 */
@Service
public class CustomerService {
    
    /**
     * Busca um cliente pelo ID.
     * 
     * @param customerId ID do cliente
     * @return Cliente encontrado
     * @throws ResourceNotFoundException se cliente não existe
     */
    public Customer getCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
```

### Commits

#### Mensagens de Commit

Usar formato semântico:

```
<tipo>(<escopo>): <assunto>

<corpo (opcional)>

<footer (opcional)>
```

**Tipos:**
- `feat`: Nova feature
- `fix`: Correção de bug
- `docs`: Mudança em documentação
- `style`: Formatação (sem lógica)
- `refactor`: Refatoração de código
- `test`: Adição/mudança de testes
- `chore`: Dependências, build, etc
- `ci`: Mudanças CI/CD

**Exemplos:**

```bash
git commit -m "feat(customer): adiciona filtro por status"
git commit -m "fix(transaction): corrige cálculo de saldo insuficiente"
git commit -m "docs(api): atualiza referência de endpoints"
git commit -m "test(wallet): adiciona testes para saque"
git commit -m "refactor(service): simplifica lógica de transferência"
```

**Corpo do Commit (opcional):**

```
feat(transaction): implementa transferência entre carteiras

Adicionado novo endpoint POST /api/transactions/transfer que permite
transferências seguras entre carteiras com validação de saldo.

Implementa:
- Validação de wallet origem e destino
- Verificação de saldo suficiente
- Transação ACID com rollback automático
- Auditoria de operação

Fecha #123
```

#### Atomic Commits

Cada commit deve ser uma unidade lógica completa:

```bash
# ✅ BOM: Cada commit é independente
git add src/main/java/com/guga/walletserviceapi/model/Customer.java
git commit -m "feat(model): adiciona validação de CPF"

git add src/main/java/com/guga/walletserviceapi/service/CustomerService.java
git commit -m "feat(service): implementa busca por CPF"

git add src/test/java/com/guga/walletserviceapi/service/CustomerServiceTest.java
git commit -m "test(service): adiciona testes para busca por CPF"

# ❌ RUIM: Múltiplas features em um commit
git add .
git commit -m "alterações varias"
```

## 🧪 Testes

### Executar Testes

```bash
# Todos os testes
./mvnw test

# Testes específicos
./mvnw test -Dtest=CustomerServiceTest

# Testes com padrão
./mvnw test -Dtest=*ServiceTest

# Apenas testes de controller
./mvnw test -Dtest=*ControllerTest

# Com cobertura
./mvnw test jacoco:report
```

### Escrever Testes

#### Estrutura AAA (Arrange, Act, Assert)

```java
@SpringBootTest
@AutoConfigureMockMvc
public class CustomerServiceTest {
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    @DisplayName("Deve salvar novo cliente corretamente")
    public void testSaveNewCustomer() {
        // Arrange (Preparar)
        Customer customer = Customer.builder()
            .name("João Silva")
            .email("joao@example.com")
            .cpf("12345678901")
            .status(Status.ACTIVE)
            .build();
        
        // Act (Agir)
        Customer savedCustomer = customerService.saveCustomer(customer);
        
        // Assert (Verificar)
        assertNotNull(savedCustomer.getCustomerId());
        assertEquals("João Silva", savedCustomer.getName());
        assertEquals(Status.ACTIVE, savedCustomer.getStatus());
    }
    
    @Test
    @DisplayName("Deve lançar exceção ao salvar cliente com email duplicado")
    public void testSaveDuplicateEmail() {
        // Arrange
        Customer customer1 = createCustomer("joao@example.com");
        customerRepository.save(customer1);
        
        Customer customer2 = createCustomer("joao@example.com");
        
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, 
            () -> customerService.saveCustomer(customer2));
    }
}
```

#### Coverage Mínimo

Objetivo: **80%** de cobertura de código

```bash
./mvnw test jacoco:report
# Abrir: target/site/jacoco/index.html
```

## 📝 Pull Request

### Criar Pull Request

1. **Fazer Push**:
```bash
git push origin feature/meu-recurso
```

2. **No GitHub**, clicar em "New Pull Request"

3. **Preencher Template**:

```markdown
## Descrição
Descrição breve do que foi mudado.

## Tipo de Mudança
- [ ] Bug fix
- [ ] Nova feature
- [ ] Mudança breaking
- [ ] Documentação

## Como Testar
Instruções para testar as mudanças:
1. ...
2. ...

## Checklist
- [ ] Código segue o padrão de estilo
- [ ] Fiz autorevisar meu código
- [ ] Comentei código complexo
- [ ] Atualizei documentação
- [ ] Novos testes foram adicionados
- [ ] Testes passam localmente
- [ ] Sem warnings do SonarQube

## Screenshots (se aplicável)
```

### Revisão de Código

Durante a revisão, espere comentários sobre:

- **Lógica**: Código funciona conforme esperado?
- **Testes**: Todos os casos cobertos?
- **Performance**: Otimizações possíveis?
- **Segurança**: Vulnerabilidades?
- **Estilo**: Segue padrões do projeto?

**Responder a Feedback:**

```bash
# Fazer alterações solicitadas
git add .
git commit -m "refactor: endereço comentários de revisão em #PR_NUMBER"
git push origin feature/meu-recurso
```

## 📦 Release

### Processo de Release

1. **Versionar** (`vX.Y.Z`):
```bash
git tag -a v0.2.4 -m "Release versão 0.2.4"
git push origin v0.2.4
```

2. **GitHub Releases**: Descrever mudanças

3. **Build & Deploy**:
```bash
./mvnw clean package -Prelease
docker build -t wallet_service:0.2.4 .
```

## 🐛 Reportar Bugs

### Antes de Reportar

1. Verificar [Issues Abertas](https://github.com/gustavo1282/wallet-service-api/issues)
2. Verificar [Discussões](https://github.com/gustavo1282/wallet-service-api/discussions)
3. Consultar [Documentação](data/docs/)

### Template de Bug

```markdown
## Descrição
Descrição clara e concisa do bug.

## Reproduzir
Passos para reproduzir:
1. ...
2. ...
3. ...

## Comportamento Esperado
O que deveria acontecer.

## Comportamento Real
O que realmente aconteceu.

## Ambiente
- OS: [e.g. Windows 10, macOS 12.1, Ubuntu 20.04]
- Java: [e.g. 21.0.1]
- Maven: [e.g. 3.9.11]
- Versão: [e.g. 0.2.4]

## Logs
```
Colar logs relevantes (se disponível)
```

## Screenshots
(se aplicável)
```

## 💡 Sugerir Features

```markdown
## Descrição
Descrição da feature desejada.

## Motivação
Por que essa feature seria útil?

## Exemplo de Uso
Como seria utilizada?

## Possível Implementação
Ideias sobre como implementar (opcional).
```

## 📚 Documentação

### Atualizar Docs

Documentação está em `data/docs/`:

- `README.md` - Guia principal
- `API_REFERENCE.md` - Endpoints
- `ARCHITECTURE_AND_DESIGN.md` - Arquitetura
- `DATA_MODEL.md` - Banco de dados
- `BUILD_AND_CI.md` - Build e CI/CD
- `SECURITY.md` - Segurança
- `OBSERVABILITY.md` - Logs e métricas

**Ao adicionar feature, atualizar docs relacionados:**

```bash
git commit -m "docs(api): documenta novo endpoint de transferência"
```

## 🔍 Code Review Checklist

Antes de fazer review, verificar:

### Funcionalidade
- [ ] Código faz o que promete
- [ ] Trata edge cases
- [ ] Não quebra features existentes

### Qualidade
- [ ] Segue padrões do projeto
- [ ] Sem código duplicado
- [ ] Métodos têm responsabilidade única

### Testes
- [ ] Novos testes adicionados
- [ ] Cobertura >= 80%
- [ ] Testes passam

### Documentação
- [ ] Javadoc para métodos públicos
- [ ] README atualizado se necessário
- [ ] Comentários explicam lógica complexa

### Segurança
- [ ] Sem SQL injection
- [ ] Sem XSS vulnerabilities
- [ ] Senhas/secrets não expostos
- [ ] Validação de input

### Performance
- [ ] Sem N+1 queries
- [ ] Sem loops ineficientes
- [ ] Cache usado corretamente

## 🎓 Recursos Úteis

- [Spring Boot Guide](https://spring.io/guides/gs/serving-web-content/)
- [JPA/Hibernate Docs](https://docs.jboss.org/hibernate/orm/)
- [Maven Documentation](https://maven.apache.org/)
- [Git Tutorial](https://git-scm.com/doc)
- [Conventional Commits](https://www.conventionalcommits.org/)

## ❓ Dúvidas?

- Abra uma [Discussion](https://github.com/gustavo1282/wallet-service-api/discussions)
- Consulte a [Documentação](data/docs/)
- Contate maintainers

## 🙏 Obrigado!

Obrigado por contribuir para tornar o Wallet Service API melhor!