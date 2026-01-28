
Boa — dá pra extrair **um plano de melhoria bem objetivo** só olhando essa tabela. Vou te ensinar como “ler” isso como dev de mercado e onde atacar primeiro.

## 1) O que cada coluna significa (na prática)

-   **Missed Instructions / Cov.**: cobertura de _bytecode_ (é a métrica principal do JaCoCo).  
    Ex: `38%` no total → pouca execução do código.
    
-   **Missed Branches / Cov.**: cobertura de decisões (`if/else`, `switch`, ternário).  
    Ex: `8%` → quase nenhum teste está passando pelos caminhos alternativos.
    
-   **Missed / Cxty**: complexidade ciclômica não coberta / total.  
    Se **Cxty alta** + **Branches baixa**, é sinal de regra com muitos caminhos sem teste.
    
-   **Missed Lines / Methods / Classes**: linhas/métodos/classes não executados.
    

> “Mercado” costuma olhar primeiro **Branches**, depois **Instructions**. Porque branch baixa indica risco real (regra de negócio com caminhos não testados).

----------

## 2) Leitura rápida do seu cenário (o diagnóstico)

### 📌 Visão geral

-   **Total Instructions: 38%** (baixo)
    
-   **Total Branches: 8%** (muito baixo)
    

Isso normalmente significa uma de duas coisas:

1.  seus testes estão mais “happy path” e não cobrem validações/erros/regras, **ou**
    
2.  uma parte importante do sistema não está sendo exercitada nos testes (ex: service/segurança).
    

E no seu relatório aparece isso bem claro:

### 🔥 O maior problema está aqui

-   `com.guga.walletserviceapi.service` → **5%** instructions, **0%** branches  
    ✅ Isso é o coração do sistema (regras). Quase não está sendo testado.
    
-   `security.auth` → **4%**
    
-   `security.jwt` → **8%**
    
-   `service.common` → **5%**
    

Enquanto isso:

-   `controller` → **81%** (alto)
    
-   `config` → **81%** (alto)
    

✅ Interpretação típica:

> você tem testes de controller (MockMvc/WebTestClient) que batem endpoint, mas as regras na camada service e fluxos de auth não estão sendo cobertos (ou estão sendo mockados demais).

----------

## 3) Como analisar “onde atacar primeiro” (priorização estilo mercado)

Use esta régua:

### Prioridade A (alto risco, alto retorno)

-   **service** (5% / 0% branches)
    
-   **security.auth / jwt** (4–8%)
    
-   **service.common** (5%)
    

👉 Isso é o que mais impacta estabilidade e bugs reais.

### Prioridade B (médio retorno)

-   helpers/audit/handlers/converters (varia)  
    Seu `helpers` tá **45%** e branches **34%** — ok-ish, mas pode melhorar depois.
    

### Prioridade C (quase ninguém cobra)

-   `model.dto` 0%
    
-   `model.request` 0%  
    DTO/request quase sempre é “dado”, não regra. Normal ignorar.
    

----------

## 4) Como usar o HTML do JaCoCo para encontrar exatamente o que falta

Quando você abre `target/site/jacoco/index.html`, faz assim:

### Passo 1: clique no package mais crítico

1.  `com.guga.walletserviceapi.service`
    
2.  Ordene por:
    
    -   **Missed Branches** (primeiro)
        
    -   depois **Cxty** (complexidade)
        

👉 Pegue as classes com **muitos branches perdidos** e **Cxty alta**.

### Passo 2: clique na classe

Você vai ver o código com cores:

-   **vermelho**: não executado
    
-   **verde**: executado
    
-   **amarelo**: parcialmente executado (geralmente branch parcial)
    

✅ Regra prática:

> Amarelo em `if/else` = você testou um lado, falta o outro.

### Passo 3: transforme amarelo/vermelho em casos de teste

Exemplos típicos (para service/auth):

-   token inválido
    
-   usuário sem role
    
-   saldo insuficiente
    
-   wallet não encontrada
    
-   customer não encontrado
    
-   validação de request (campos nulos, limites, etc.)
    

----------

## 5) O que sua tabela já está te dizendo (ações concretas)

### 5.1 Corrigir o “desbalanceamento” Controller alto vs Service baixo

Se controller está 81% mas service 5%, quase sempre você está:

-   mockando o service nos testes de controller (`@WebMvcTest` + `@MockBean`), **ou**
    
-   testando o controller sem deixar o fluxo real do service acontecer
    

✅ Plano padrão:

-   manter testes de controller (ok)
    
-   **criar testes de service** com:
    
    -   `@ExtendWith(MockitoExtension.class)` (unit)
        
    -   ou `@SpringBootTest` com H2 (integração leve) quando precisa de JPA
        

### 5.2 Levantar Branches

Seu branches total 8% é um “alerta vermelho”.  
A forma mais eficiente de subir branches é:

-   para cada método importante, escrever **3 testes**:
    
    1.  happy path
        
    2.  validação/erro esperado
        
    3.  caso limite (edge case)
        

Isso sozinho sobe branches brutalmente.

----------

## 6) Um “plano de melhoria” rápido (7 passos)

1.  **Service**: escolha 3 serviços mais críticos (ex: AuthService, WalletService, TransactionService)
    
2.  Para cada um, pegue 2 métodos com maior Cxty (no HTML)
    
3.  Crie testes cobrindo:
    
    -   sucesso
        
    -   erro de negócio
        
    -   validação/edge case
        
4.  **Security**: crie testes pequenos para:
    
    -   token inválido
        
    -   token expirado
        
    -   role insuficiente
        
5.  Rode:
    
    `mvn clean verify` 
    
6.  Abra o HTML e compare:
    
    -   branches do service subiu?
        
    -   security subiu?
        
7.  Só depois pense em DTO/request.
    

----------

## 7) Como “market fit” costuma ser medido (sem paranoia)

Muitas equipes miram:

-   **Service**: 60–80% lines, branches subindo
    
-   **Controller**: 40–70% (depende)
    
-   **Config/DTO**: não é foco (excluem no Sonar/JaCoCo)
    

Você já está com controller alto. Agora é puxar o service.

----------

Se você me disser **quais são seus services principais** (nomes das classes) ou colar o print/trecho da lista de classes dentro de `com.guga.walletserviceapi.service` no HTML, eu monto um plano de ataque com:

-   3 classes prioritárias
    
-   métodos específicos
    
-   quais casos de teste escrever para transformar cada trecho amarelo em verde.