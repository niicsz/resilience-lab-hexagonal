# Resilient Order Service — Laboratório de Resiliência

Lab local (Java 25 + Spring Boot 4.1.0 + Resilience4j) que demonstra, de forma
mensurável no Grafana, seis padrões de resiliência. Cada experimento roda um par
**baseline** (padrão desligado) vs **resilient** (padrão ligado).

> **Novo por aqui?** Se você não conhece os padrões de resiliência, comece pela
> seção [Os padrões de resiliência explicados](#os-padrões-de-resiliência-explicados)
> — cada um é descrito em linguagem simples, com uma analogia e o problema que resolve.

## Subir o ambiente

```bash
make up      # app :8080 · wiremock-admin :8081/__admin · prometheus :9090 · grafana :3000
```

Abra o Grafana em http://localhost:3000 (acesso anônimo) → dashboard
**"Order Service — Resilience"**.

> MySQL publica na porta **3307** do host (3306 costuma estar ocupado). O app usa
> `mysql:3306` na rede interna — nada muda para os experimentos.

## Rodar os experimentos

```bash
make scenario-1     # ou: scripts/run-scenario.sh 1 [baseline|resilient|both]
...
make all            # roda os seis
make down
```

Cada execução imprime marcadores `>>> WINDOW <modo> START/END <iso8601>` — use-os
para localizar as janelas no Grafana (baseline primeiro, depois resilient).

## Os padrões de resiliência explicados

Em um sistema distribuído, o `order-service` depende de outros serviços (pagamento,
estoque, rastreio, notificação). Qualquer um deles pode ficar **lento** ou **falhar**.
Sem proteção, essa falha "vaza" e derruba o serviço inteiro. Os padrões abaixo são
formas de conter esse estrago. Todos vêm da biblioteca **Resilience4j**.

### 1. Timeout — "não espero para sempre"

Define um tempo máximo para uma chamada externa. Se o serviço de pagamento não
responder em **800ms**, a espera é abortada em vez de travar a thread esperando.

- **Problema que resolve:** uma dependência lenta prende as threads do seu serviço.
  Se todas as threads ficam esperando, o serviço para de atender **todo mundo** —
  inclusive quem nem usa o pagamento.
- **Analogia:** você liga para um fornecedor; se ninguém atende em 30s, você desliga
  e segue a vida, em vez de ficar com o telefone no ouvido o dia todo.

### 2. Retry com backoff exponencial e jitter — "tento de novo, com calma"

Falhas de rede costumam ser passageiras. O Retry **repete** a chamada que falhou
(até **3 tentativas**). Entre tentativas ele espera cada vez mais (**backoff
exponencial**: 200ms → 400ms → 800ms) e adiciona uma variação aleatória
(**jitter**) nesse tempo.

- **Problema que resolve:** um soluço momentâneo (um `503` isolado) não deveria
  virar um erro para o usuário.
- **Por que backoff + jitter?** Se todos os clientes tentassem de novo ao mesmo
  tempo, dariam uma "martelada" no serviço já fragilizado (efeito manada). O backoff
  espalha as tentativas no tempo; o jitter evita que todas caiam no mesmo instante.
- **Analogia:** o telefone deu ocupado. Você não redisca sem parar — espera um
  pouco, depois um pouco mais, e não exatamente junto com todo mundo.

### 3. Bulkhead (isolamento por pool de threads) — "compartimentos estanques"

Reserva um **pool de threads separado** para chamar um serviço específico (aqui, o
de rastreio: **4 threads, fila de 2**). Se esse serviço fica lento, ele esgota
apenas o próprio pool — não as threads que atendem os outros fluxos.

- **Problema que resolve:** um recurso não-crítico e lento (rastreio) não pode
  consumir todas as threads e derrubar o fluxo crítico (fechar o pedido).
- **Analogia:** o nome vem dos compartimentos estanques de um navio. Se um
  compartimento inunda, as paredes impedem que a água tome o navio todo.

### 4. Circuit Breaker (disjuntor) — "paro de bater na porta fechada"

Monitora a taxa de erro das chamadas. Se numa janela de **20 chamadas** mais de
**50%** falham, o circuito **"abre"**: por **10 segundos** as chamadas nem tentam
sair — falham na hora (*fail fast*). Depois ele entra em **half-open**, deixa passar
**3 chamadas** de teste; se derem certo, **"fecha"** e volta ao normal.

- **Problema que resolve:** insistir em um serviço que está claramente fora do ar só
  gasta recursos, acumula latência e atrasa a recuperação dele.
- **Os três estados:** **fechado** (tudo normal, chamadas passam) → **aberto**
  (falha rápido, sem tentar) → **half-open** (testa as águas antes de voltar).
- **Analogia:** o disjuntor da sua casa. Diante de um curto, ele desarma para
  proteger a instalação, em vez de deixar a corrente queimar tudo.

### 5. Circuit Breaker protegendo o Retry — "os dois juntos, na ordem certa"

Retry e Circuit Breaker combinados. A ordem importa: o Retry fica "por fora" e o
Circuit Breaker "por dentro". Assim, quando o CB está **aberto**, o Retry recebe uma
recusa imediata e **não fica multiplicando** chamadas contra um serviço caído.

- **Problema que resolve:** Retry sozinho, durante um outage prolongado, **amplifica**
  a carga (cada requisição vira 3). O CB corta isso pela raiz.
- **Analogia:** não adianta tentar ligar 3 vezes seguidas se você já sabe que a linha
  está fora do ar — o disjuntor te avisa para nem discar.

### 6. Fallback — "plano B em vez de erro"

Quando uma funcionalidade **não-essencial** falha, em vez de quebrar o pedido inteiro
o serviço executa uma alternativa segura. Aqui: se a **notificação** falha, o pedido
segue como `CONFIRMED` e a notificação é apenas adiada.

- **Problema que resolve:** uma parte secundária (avisar o cliente) não deveria
  impedir o principal (registrar a compra) de acontecer — isso é **degradação
  graciosa**.
- **Analogia:** a maquininha do cartão está sem papel para o comprovante. A venda é
  concluída assim mesmo; o comprovante vai por e-mail depois.

## Os seis experimentos e o que observar

| # | Padrão | Injeção (WireMock) | Baseline (padrão OFF) | Resilient (padrão ON) | Painel |
|---|--------|--------------------|-----------------------|-----------------------|--------|
| 1 | Timeout | Delay crescente no payment (0→2.5s) | p99 de `/orders` acompanha o delay | p99 controlado; timeouts disparam | Latência; Retries e Timeouts |
| 2 | Retry + backoff/jitter | inventory 503 intermitente | pedidos viram `OUT_OF_STOCK` | recupera via retry | Retries e Timeouts |
| 3 | ThreadPoolBulkhead | tracking lento (4s) sob flood | checkout degrada (threads presas) | checkout estável; tracking 503 rápido | Bulkhead; Latência |
| 4 | CircuitBreaker | payment 500 por ~40s, depois cura | erros acumulam durante todo o outage | CB abre→half-open→fecha; fail fast | Estado do Circuit Breaker |
| 5 | CB protege o retry | payment em outage | retry ON + CB OFF: chamadas explodem | CB ON: curto-circuita (menos chamadas) | Estado do CB; log do k6 |
| 6 | Fallback | notification 500 | pedido quebra (`/orders` 5xx) | pedido segue `CONFIRMED` | Taxa de erro por endpoint |

## Como funciona

- **Toggles por env** (`RESILIENCE_*_ENABLED`): o orquestrador recria o container
  `order-service` com o padrão sob teste ligado/desligado; todo o resto fica ligado.
- **Mudança de comportamento em runtime**: cada script k6 tem um "timeline" (1 VU)
  que injeta/cura a falha via **WireMock Admin API** em offsets fixos — reprodutível.
- **Métricas**: Prometheus faz scrape do `/actuator/prometheus` do app; o k6
  também faz remote-write das próprias métricas para o Prometheus.

## Arquitetura (hexagonal)

O `order-service` segue portas e adapters: domínio e casos de uso são Java puro,
e Spring, JPA, RestClient e Resilience4j existem somente nos adapters e em `config/`.

    adapters/inbound/web + scheduling
                     |
    application/ports/inbound
                     |
    application/usecases + domain
                     |
    application/ports/outbound
                     |
    adapters/outbound/http (+ resilience) + persistence

| Pacote | Conteúdo |
|--------|----------|
| `domain` | `Order` com as transições de status, `PaymentOutcome`, `TrackingInfo` e exceções de domínio |
| `application/ports/inbound` | `OrderUseCasePort`, `PaymentReconciliationUseCasePort` |
| `application/ports/outbound` | `OrderRepositoryPort`, `InventoryPort`, `PaymentGatewayPort`, `NotificationPort`, `TrackingPort` |
| `application/usecases` | `OrderUseCase`, `PaymentReconciliationUseCase` (instanciados em `config/BeanConfiguration`) |
| `adapters/inbound/web` | `OrderController`, DTOs e `GlobalExceptionHandler` |
| `adapters/inbound/scheduling` | `PaymentReconciliationScheduler` |
| `adapters/outbound/http` | clientes `@HttpExchange` e os adapters `Http*Adapter` que implementam as portas |
| `adapters/outbound/resilience` | `ResilienceFacade`: timeout, retry, circuit breaker, bulkhead e fallback |
| `adapters/outbound/persistence` | `OrderEntity` (JPA), `SpringDataOrderRepository` e `OrderRepositoryAdapter` |

Onde fica cada decisão:

- **Resiliência é infraestrutura.** Os adapters HTTP aplicam os padrões pelo
  `ResilienceFacade` e traduzem as falhas para exceções de domínio
  (`ExternalServiceUnavailableException`, `DependencyOverloadedException`).
- **Regra de negócio fica no caso de uso.** "Gateway de pagamento indisponível → pedido
  `PAYMENT_PENDING` para reconciliar depois" e "estoque indisponível → `OUT_OF_STOCK`"
  estão em `OrderUseCase`, testados com fakes em memória, sem Resilience4j.
- **As regras de dependência são verificadas** pelo `ArchitectureTest` (ArchUnit): o
  domínio só depende de `java..`, a aplicação só do domínio, e os adapters acessam os
  casos de uso apenas pelas portas.

### Testes

    cd order-service && ./mvnw test

Os testes de persistência e o de fluxo completo usam Testcontainers (Docker precisa
estar rodando). O build formata o código com google-java-format (`fmt-maven-plugin`).
