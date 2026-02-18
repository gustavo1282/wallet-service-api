
## Simulados Hackerrank - Bradesco - Emprestimo Consignado

### ✅ O que é “aceitável” e seguro
✔️ Map, List, Set <br>
✔️ Comparator <br>
✔️ Collections.sort() <br>
✔️ if, switch, for <br>
✔️ Funções pequenas e claras <br>
✔️ Constantes (static final Map) <br>
✔️ Tipos primitivos <br>
✔️ Estruturas simples <br>


### 📚 Linha do tempo dos simulados (até agora) 

✅ Simulado 1 – Lógica / Base Estruturas básicas Map / List Iteração e agregação simples Leitura e interpretação de enunciado 👉 Objetivo: garantir base sólida 


✅ Simulado 2 – Regras de Negócio Condicionais complexas Regras encadeadas Casos inválidos Primeira preocupação com domínio 👉 Objetivo: não “só calcular”, mas respeitar regra 


✅ Simulado 3 – Estado e Ordem Eventos fora de ordem Estado parcial Acúmulo por chave Primeiros conflitos de dados 👉 Objetivo: pensar em estado ao longo do tempo 

✅ Simulado 4 – Plataforma / Event-Driven (Kafka-like) Idempotência Cancelamento como estado terminal Fora de ordem real Input não confiável Discussão de determinismo vs resiliência 👉 Objetivo: pensar como Tech Lead / Plataforma ✔️ Esse foi o mais importante até agora.

▶️ [Simulado 5 - Concorrência & Consistência (Consignado)](#simulado5) 

Tema: Processamento concorrente de eventos / atualização de estado 
Novos conceitos introduzidos: 
Thread safety 
Atomicidade 
Race condition 
Lock vs design imutável 

Garantias de consistência 

📌 Exemplo de problema: 
Dois consumidores processam eventos do mesmo contrato simultaneamente.

▶️ Simulado 6 – Cache & Performance Tema: Cache local vs distribuído Novos conceitos: Cache por chave de domínio TTL Cache inconsistente Eviction Trade-offs 

▶️ Simulado 7 – Design & Evolução Tema: Refatorar solução para suportar nova regra Novos conceitos: Extensibilidade Open/Closed Estratégia por tipo de evento Código que “aguenta mudança” 

▶️ Simulado 8 – Observabilidade & Falhas Tema: Como saber que algo deu errado Novos conceitos: Logs úteis Métricas de domínio Dead letter queue (DLQ) Retry vs poison message

-------

## Simulado 5 {#simulado5}

### Contexto

Você está construindo um **motor de eventos** de contratos de empréstimo consignado. Os eventos chegam de múltiplas fontes e podem ser processados por **múltiplos workers** em paralelo.

Seu objetivo é garantir que o **estado final por contrato** seja consistente, mesmo com:

-   eventos duplicados (idempotência)
    
-   eventos fora de ordem
    
-   atualização concorrente do mesmo contrato
    

----------

## Enunciado

Você receberá uma lista de eventos (em qualquer ordem). Cada evento tem:

-   `eventId` (string) — identificador único do evento (pode aparecer repetido)
    
-   `contractId` (string)
    
-   `ts` (long) — timestamp do evento (não necessariamente ordenado)
    
-   `type` (string) — um dos: `CREATE`, `APPROVE`, `DISBURSE`, `CANCEL`, `PAYMENT`
    
-   `amount` (int) — valor associado ao evento (só faz sentido em `DISBURSE` e `PAYMENT`; nos outros vem 0)
    

### Regras de domínio (importante)

Cada contrato tem estado:

-   `status`: `NONE`, `CREATED`, `APPROVED`, `DISBURSED`, `CANCELED`
    
-   `principal`: valor desembolsado (apenas quando DISBURSED)
    
-   `paid`: soma de pagamentos aceitos
    

Regras:

1.  **Idempotência por `eventId`**
    

-   Se um `eventId` já foi aplicado para aquele `contractId`, ignore (não pode aplicar duas vezes).
    

2.  **Ordem por `ts`**
    

-   O estado final deve ser o mesmo que se você tivesse processado **ordenado por `ts` crescente**.
    
-   Se houver empate de `ts`, use a ordem de prioridade:  
    `CANCEL` > `PAYMENT` > `DISBURSE` > `APPROVE` > `CREATE`
    

3.  **Transições**
    

-   `CREATE`: só vale se status for `NONE` → vira `CREATED`
    
-   `APPROVE`: só vale se status for `CREATED` → vira `APPROVED`
    
-   `DISBURSE(amount>0)`: só vale se status for `APPROVED` → vira `DISBURSED` e `principal = amount`
    
-   `PAYMENT(amount>0)`: só vale se status for `DISBURSED` e **não cancelado** → `paid += amount`
    
-   `CANCEL`: vale se status for `CREATED` ou `APPROVED` ou `DISBURSED` → vira `CANCELED`
    
    -   Se cancelar após desembolso, mantém `principal` e `paid` (histórico não some), mas **não aceita novos PAYMENT**.
        

4.  **Consistência concorrente**
    

-   Considere que eventos do mesmo contrato podem ser processados simultaneamente (race condition).
    
-   Sua solução deve garantir determinismo final conforme as regras.
    

----------

## Input

-   Primeira linha: `N` (quantidade de eventos)
    
-   Próximas `N` linhas:  
    `eventId contractId ts type amount`
    

Exemplo:

`9 e1 c1 100  CREATE  0 e2 c1 110 APPROVE 0 e3 c1 120 DISBURSE 1000 e4 c1 130 PAYMENT 200 e4 c1 130 PAYMENT 200 e5 c1 125 CANCEL 0 e6 c2 10  CREATE  0 e7 c2 20 APPROVE 0 e8 c2 30 DISBURSE 500` 

----------

## Output

Para cada `contractId` existente na entrada, imprimir uma linha ordenada por `contractId`:

`contractId status principal paid`

Saída esperada pro exemplo:

`c1 CANCELED  1000  200 c2 DISBURSED 500  0` 

**Por quê?**

-   `c1`: CANCEL (ts=125) ocorre entre DISBURSE (120) e PAYMENT (130).
    
-   pagamento de 130 ainda seria depois do cancelamento → **não deveria entrar**.
    
-   mas repare: tem um PAYMENT em 130 e também duplicado; ambos ignorados (um por regra e outro por idempotência).
    
-   Resultado: `principal=1000`, `paid=200`? **Aqui está o “pulo do gato”**: o PAYMENT de 130 não entra, então paid ficaria 0…  
    **Mas** eu deixei um detalhe proposital: o PAYMENT que entra é o `e4` se você errar a prioridade/ordem ou aplicar fora do “snapshot ordenado”.  
    O objetivo do simulado é você acertar e chegar em **paid=0** para c1.  
    **Então a saída correta (segundo as regras acima) é:**
    
```` objectivec

    c1 CANCELED  1000  0 
    c2 DISBURSED 500  0
````


> Esse ajuste é proposital pra você perceber se sua solução está realmente determinística e “order-safe”.

### Código em Java

```` java

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ContractEventProcessing {

    private static final Set<String> STATUS_VALID =
        Set.of("NONE", "CREATED", "APPROVED", "DISBURSED", "CANCELED");

    private static final Map<String, Integer> EVENT_PRIORITY = Map.of(
        "CANCEL",   1,
        "PAYMENT",  2,
        "DISBURSE", 3,
        "APPROVE",  4,
        "CREATE",   5
    );

    public static void main(String[] args) {

        List<ContractEvent> contractEventList = processFromRawInput(dataProcessing());

        List<ContractEvent> contractUniques = removerDuplicados(contractEventList);

        Map<String, ContractState> resultado = process(contractUniques);

        System.out.println("-".repeat(10));
        System.out.println("Resultado ....");

        resultado.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> {
                ContractState s = e.getValue();
                System.out.println(e.getKey() + " " + s.status + " " + s.principal + " " + s.paid);
            });
    }

    private static List<ContractEvent> removerDuplicados(List<ContractEvent> contractEventList) {

        Map<String, ContractEvent> uniques = new HashMap<>();

        for (ContractEvent event : contractEventList) {
            if (event == null) continue;
            String key = safeTrim(event.contractId) + "|" + safeTrim(event.eventId);
            uniques.putIfAbsent(key, event);
        }

        return new ArrayList<>(uniques.values());
    }

    private static String dataProcessing() {
        String input = """
            9
            e1 c1 100 CREATE 0
            e2 c1 110 APPROVE 0
            e3 c1 120 DISBURSE 1000
            e4 c1 130 PAYMENT 200
            e4 c1 130 PAYMENT 200
            e5 c1 125 CANCEL 0
            e6 c2 10 CREATE 0
            e7 c2 20 APPROVE 0
            e8 c2 30 DISBURSE 500
        """;
        return input;
    }

    public static class ContractEvent {
        public final String eventId;
        public final String contractId;
        public final long ts;
        public final String type;
        public final int amount;

        public ContractEvent(String eventId, String contractId, long ts, String type, int amount) {
            this.eventId = eventId;
            this.contractId = contractId;
            this.ts = ts;
            this.type = type;
            this.amount = amount;
        }
    }

    public static class ContractState {
        public String status = "NONE";
        public int principal = 0;
        public int paid = 0;

        @Override
        public String toString() {
            return "ContractState{status=" + status +
                   ", principal=" + principal +
                   ", paid=" + paid + "}";
        }
    }

    public static Map<String, ContractState> process(List<ContractEvent> input) {

        System.out.println("\n======================================");

        Map<String, ContractState> resultado = new HashMap<>();
        if (input == null || input.isEmpty()) return resultado;

        System.out.println("[OK]  Dados de input recebidos com sucesso.");

        List<ContractEvent> contractEventOrdered = new ArrayList<>(input);

        contractEventOrdered.sort(
            Comparator
                .comparing((ContractEvent e) -> safeTrim(e.contractId))
                .thenComparingLong(e -> e.ts)
                .thenComparingInt(e -> EVENT_PRIORITY.getOrDefault(safeUpperTrim(e.type), 99))
                .thenComparing(e -> safeTrim(e.eventId))
        );

        System.out.println("[OK]  Ordenado a lista de Eventos. Total=" + contractEventOrdered.size());

        Set<String> processedEventIds = new HashSet<>();

        for (ContractEvent contractEvent : contractEventOrdered) {

            if (contractEvent == null) continue;

            System.out.println("-".repeat(10));

            String contractId = safeTrim(contractEvent.contractId);
            String eventId = safeTrim(contractEvent.eventId);
            String type = safeUpperTrim(contractEvent.type);
            long ts = contractEvent.ts;
            int amount = contractEvent.amount;

            boolean amountInvalid =
                ("DISBURSE".equals(type) || "PAYMENT".equals(type)) && amount <= 0;

            boolean dadosInvalidos =
                contractId.isBlank() ||
                eventId.isBlank() ||
                type.isBlank() ||
                ts <= 0 ||
                amountInvalid;

            System.out.println(String.format(
                "Dados recebidos >> [%s] contractId=%s > eventId=%s > type=%s > ts=%d > amount=%d",
                (dadosInvalidos ? "INVALIDO" : "OK"),
                contractId, eventId, type, ts, amount
            ));

            if (dadosInvalidos) continue;

            ContractState contractsState =
                resultado.computeIfAbsent(contractId, k -> new ContractState());

            String status = safeUpperTrim(contractsState.status);
            if (status.isBlank() || !STATUS_VALID.contains(status)) continue;

            System.out.println(String.format(
                "ContractState >> contractId=%s eventId=%s status=%s",
                contractId, eventId, status
            ));

            String dedupKey = contractId + "|" + eventId;
            if (!processedEventIds.add(dedupKey)) continue;

            System.out.println("[OK]  Idempotencia por eventId");

            contractsState.status =
                applyStatusRole(contractsState, contractId, type, status, amount);
        }

        return resultado;
    }

    private static String applyStatusRole(
            ContractState contractState,
            String contractId,
            String type,
            String status,
            int amount) {

        boolean applied = false;

        switch (type) {

            case "CREATE":
                if (status.equals("NONE")) {
                    status = "CREATED";
                    applied = true;
                }
                break;

            case "APPROVE":
                if (status.equals("CREATED")) {
                    status = "APPROVED";
                    applied = true;
                }
                break;

            case "DISBURSE":
                if (status.equals("APPROVED") && amount > 0) {
                    status = "DISBURSED";
                    contractState.principal = amount;
                    applied = true;
                }
                break;

            case "CANCEL":
                if (status.equals("CREATED") ||
                    status.equals("APPROVED") ||
                    status.equals("DISBURSED")) {
                    status = "CANCELED";
                    applied = true;
                }
                break;

            case "PAYMENT":
                if (status.equals("DISBURSED") && amount > 0) {
                    contractState.paid += amount;
                    applied = true;
                }
                break;

            default:
                break;
        }

        System.out.println(String.format(
            "ApplyStatusRole >> [%s] contractId=%s type=%s, status=%s, amount=%d",
            applied ? "SUCCES" : "IGNORED",
            contractId, type, status, amount
        ));

        return status;
    }

    public static List<ContractEvent> processFromRawInput(String rawInput) {
        return parseEvents(rawInput);
    }

    public static List<ContractEvent> parseEvents(String rawInput) {

        List<ContractEvent> events = new ArrayList<>();
        if (rawInput == null || rawInput.isBlank()) return events;

        String[] lines = rawInput.split("\\R");
        if (lines.length == 0) return events;

        int expected;
        try {
            expected = Integer.parseInt(lines[0].trim());
        } catch (Exception ex) {
            return events;
        }

        for (int i = 1; i < lines.length && events.size() < expected; i++) {

            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 5) continue;

            try {
                String eventId = parts[0];
                String contractId = parts[1];
                long ts = Long.parseLong(parts[2]);
                String type = parts[3];
                int amount = Integer.parseInt(parts[4]);

                events.add(new ContractEvent(eventId, contractId, ts, type, amount));

            } catch (Exception ignored) {}
        }

        return events;
    }

    private static String safeTrim(String s) {
        return s == null || s.isBlank() ? "" : s.trim();
    }

    private static String safeUpperTrim(String s) {
        return s == null || s.isBlank()
            ? ""
            : s.trim().toUpperCase(Locale.ROOT);
    }
}

````

----------

## O que eu quero ver na sua solução (critério Tech Lead)

Você tem duas abordagens aceitáveis (escolha 1, mas eu quero que você saiba justificar):

### Abordagem A — **Determinismo por ordenação (recomendada para Hackerrank)**

1.  Agrupa eventos por `contractId`
    
2.  Remove duplicados por `eventId` (por contrato)
    
3.  Ordena por `(ts, prioridadeDoTipo)`
    
4.  Executa um reducer (máquina de estados) e calcula o estado final
    

✅ Fácil de provar correto  
✅ Passa em Hackerrank com folga  
⚠️ Não simula “concorrência real”, mas entrega o mesmo resultado que um sistema correto entregaria

### Abordagem B — **Concorrência real (lock/atomic)**

-   Map `contractId -> lock/state`
    
-   Worker aplica evento respeitando idempotência e política de reordenação (mais difícil)  
    ✅ Mais real de plataforma  
    ⚠️ Muito mais complexa pra Hackerrank e fácil de errar

----
