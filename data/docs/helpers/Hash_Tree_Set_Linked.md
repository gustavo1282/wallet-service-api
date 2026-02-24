
## 1. Tabela Comparativa de Performance e Comportamento

| Estrutura  | Ordenação  | Permite Nulo?  | Performance (Busca/Inserção)  | Melhor Caso de Uso |
|--|--|--|--|--|
| **HashMap** | Nenhuma | Sim | O(1) - Constante | Quando a velocidade é a prioridade e a ordem não importa. |
| **TreeMap** | Natural (ou Comparator) | Não (na chave) | O(log n) - Logarítmica | Quando você precisa dos dados sempre ordenados. | 
| **HashSet** | Nenhuma | Sim | O(1) - Constante | Para garantir que não existam elementos duplicados. Set<Object> var = HashMap<>(Object) | 
| **TreeSet** | Natural | Não | O(log n) - Logarítmica | Conjunto de elementos únicos e sempre ordenados. | 
| **LinkedHashMap** | Ordem de Inserção | Sim | O(1) | Quando a ordem de chegada dos dados é importante (ex: Cache). |

----------

### Dicas para o seu arquivo `.md`:

----------

## 2. Aplicabilidade: Quando usar qual?

### **Map (Dicionário Chave-Valor)**

Imagine que você está processando o Onboarding e precisa buscar o `Customer` rapidamente pelo `UUID`.

-   **Use `HashMap`:** É o "padrão ouro". Busca quase instantânea.
    
-   **Use `TreeMap`:** Se você precisar listar os clientes por ordem alfabética de nome direto da memória sem reordenar.
    

### **Set (Conjuntos Únicos)**

Imagine que você recebe uma lista de CPFs processados no Kafka e quer remover os duplicados.

-   **Use `HashSet`:** Adicione todos lá. No final, você terá apenas CPFs únicos.
    

### **O "Monstro": `Map<String, Set<String>>`**

Isso é muito comum em desafios e na vida real. Exemplo: **Agrupar IDs de clientes por Status.**

-   A chave (`String`) é o `Status` (ACTIVE, REJECTED).
    
-   O valor (`Set<String>`) é a lista de `IDs` únicos naquele status.
    

----------

## 3. Como Iterar (Melhores Práticas Java 21)

Esqueça os `for` antigos. Com Java 8+ e as melhorias do 21, usamos abordagens mais limpas:

### Iterando um Map (Chave e Valor)

Java

``` java

Map<String, Integer> scores = new HashMap<>();
scores.put("Gustavo", 95);

// Forma moderna e performática
scores.forEach((key, value) -> {
    System.out.println("Nome: " + key + " | Score: " + value);
});

```

### Iterando um Set

Java

``` java

Set<String> cpfs = new HashSet<>();
cpfs.stream()
    .filter(cpf -> cpf.startsWith("123")) // Exemplo de processamento
    .forEach(System.out::println);

```

----------

## 4. Dica de Arquiteto: Performance $O(1)$ vs $O(\log n)$

-   **$O(1)$ (HashMap):** Não importa se você tem 10 ou 1 milhão de clientes, o tempo de busca é praticamente o mesmo.
    
-   **$O(\log n)$ (TreeMap):** Conforme a lista cresce, o tempo de busca aumenta (embora devagar).
    

> **Atenção no Onboarding:** Se você estiver criando um cache em memória para validar CPFs duplicados em tempo real no Kafka, use **`HashSet`**. É o mais performático para checagens rápidas de existência (`contains`).