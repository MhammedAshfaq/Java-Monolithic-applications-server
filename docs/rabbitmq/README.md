# RabbitMQ Messaging

Asynchronous messaging layer using RabbitMQ with **domain-driven** folder structure.
Each domain (email, notification, audit, task) is fully self-contained — its queue config,
DLQ, publisher, listener, and message DTO all live in one folder.

---

## Architecture

```
                        ┌─────────────────┐
  Business Service ──►  │   Publisher      │  ── convertAndSend ──►  RabbitMQ Exchange
                        └─────────────────┘                             │
                                                                        │ routing key
                                                                        ▼
                                                                    ┌────────┐
                                                                    │ Queue  │
                                                                    └───┬────┘
                                                                        │
                        ┌─────────────────┐                             │
                        │   Listener       │  ◄── @RabbitListener ──────┘
                        └────────┬────────┘
                                 │
                                 ▼
                         Business Service          (success → ACK)
                                 │
                                 ▼ (failure after retries)
                        ┌─────────────────┐
                        │  Dead Letter Q   │  ── inspect in RabbitMQ UI
                        └─────────────────┘
```

---

## Folder Structure

```
config/rabbitmq/                           ← Shared infrastructure only
├── RabbitMQConfig.java                    # Exchanges (main + DLX), JSON converter, RabbitTemplate
└── RabbitMQProperties.java                # Custom app-level properties (@ConfigurationProperties)

messaging/
├── constant/
│   └── QueueConstants.java                # All exchange/queue/routing key/DLQ names
│
├── email/                                 ← Everything email in one place
│   ├── EmailQueueConfig.java              # Queue + DLQ + bindings
│   ├── EmailMessage.java                  # DTO (to, subject, body, template)
│   ├── EmailPublisher.java                # Sends to app.email.send
│   └── EmailListener.java                 # Consumes from app.email.send
│
├── notification/                          ← 3 notification types
│   ├── NotificationQueueConfig.java       # All 3 queues + DLQs + bindings
│   ├── publisher/
│   │   └── NotificationPublisher.java     # Single publisher, routes by type
│   ├── listener/
│   │   ├── SingleNotificationListener.java    # ← app.notification.single
│   │   ├── MulticastNotificationListener.java # ← app.notification.multicast
│   │   └── TopicNotificationListener.java     # ← app.notification.topic
│   ├── message/
│   │   └── NotificationMessage.java       # Unified DTO with factory methods
│   └── constant/
│       └── NotificationConstants.java     # Types, channels, priorities
│
├── audit/                                 ← Everything audit in one place
│   ├── AuditQueueConfig.java              # Queue + DLQ + bindings
│   ├── AuditMessage.java                  # DTO (action, userId, ip, details, timestamp)
│   ├── AuditPublisher.java                # Sends to app.audit.event
│   └── AuditListener.java                 # Consumes from app.audit.event
│
├── dlq/                                   ← Shared DLQ processing + Slack alerts
│   ├── DlqListener.java                  # Listens to ALL DLQ queues
│   ├── DlqMessage.java                   # Unified DLQ payload record
│   ├── DlqNotificationService.java       # Identifies source, dispatches alert + DB
│   └── DlqSlackNotifier.java             # Sends Slack webhook with formatted alert
│
└── task/                                  ← Everything task in one place
    ├── TaskQueueConfig.java               # Queue + DLQ + bindings
    ├── TaskMessage.java                   # DTO (type, payload)
    ├── TaskPublisher.java                 # Sends to app.task.general
    └── TaskListener.java                  # Consumes from app.task.general
```

### What lives where

| Concern | Location | Why |
|---------|----------|-----|
| Exchanges (main + DLX) | `config/rabbitmq/RabbitMQConfig.java` | Shared by all domains |
| JSON converter + RabbitTemplate | `config/rabbitmq/RabbitMQConfig.java` | Shared by all domains |
| Queue + DLQ + Bindings | `messaging/{domain}/XxxQueueConfig.java` | Domain-specific, self-contained |
| Message DTO | `messaging/{domain}/XxxMessage.java` | Domain-specific |
| Publisher | `messaging/{domain}/XxxPublisher.java` | Domain-specific |
| Listener | `messaging/{domain}/XxxListener.java` | Domain-specific |
| Queue/routing key names | `messaging/constant/QueueConstants.java` | Single source of truth, no hardcoded strings |

---

## Queues & Routing

| Domain | Queue | Routing Key | DLQ |
|--------|-------|-------------|-----|
| Email | `app.email.send` | `app.email.send` | `app.email.send.dlq` |
| Notification (Single) | `app.notification.single` | `app.notification.single` | `app.notification.single.dlq` |
| Notification (Multicast) | `app.notification.multicast` | `app.notification.multicast` | `app.notification.multicast.dlq` |
| Notification (Topic) | `app.notification.topic` | `app.notification.topic` | `app.notification.topic.dlq` |
| Audit | `app.audit.event` | `app.audit.event` | `app.audit.event.dlq` |
| Task | `app.task.general` | `app.task.general` | `app.task.general.dlq` |

**Exchanges:**
- `app.exchange` — main topic exchange for all messages
- `app.exchange.dlx` — dead letter exchange for failed messages

---

## Usage Examples

### Send an Email

```java
@RequiredArgsConstructor
@Service
public class UserService {

    private final EmailPublisher emailPublisher;

    public void register(User user) {
        // ... save user ...
        emailPublisher.send(user.getEmail(), "Welcome!", "Thanks for signing up.");
    }
}
```

### Send Notifications (single publisher, 3 types)

```java
// Single — one user
notificationPublisher.sendSingle(userId, "Order shipped", "Your order is on the way", "PUSH");

// Multicast — list of users
notificationPublisher.sendMulticast(List.of("u1", "u2", "u3"), "Team Update", "Sprint review at 3pm", "IN_APP");

// Topic — all subscribers of a topic
notificationPublisher.sendTopic("product-updates", "New Feature!", "Dark mode is here", "PUSH");

// Topic with metadata (e.g. deep link)
notificationPublisher.sendTopic("deals", "Flash Sale", "50% off", "PUSH", Map.of("url", "/deals"));

// Generic — using factory methods on NotificationMessage
notificationPublisher.send(NotificationMessage.single(userId, "Hello", "World", "PUSH"));
notificationPublisher.send(NotificationMessage.multicast(userIds, "Alert", "Check this", "IN_APP"));
notificationPublisher.send(NotificationMessage.topic("system", "Maintenance", "At 2am", "PUSH"));
```

### Send an Audit Event

```java
auditPublisher.send("USER_REGISTERED", userId, Map.of("email", email));

// With IP address
auditPublisher.send("LOGIN_FAILED", null, request.getRemoteAddr(), Map.of("email", email));
```

### Send a Background Task

```java
taskPublisher.send("GENERATE_REPORT", Map.of("month", "January", "format", "PDF"));
```

---

## Configuration

### application.yaml

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    connection-timeout: 10000
    publisher-confirm-type: correlated    # Ensures messages reach the broker
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: auto            # Spring manages ACK/NACK automatically
        prefetch: 1                       # Fair dispatch
        concurrency: 1
        max-concurrency: 3
        retry:
          enabled: true
          initial-interval: 1000          # 1st retry delay: 1s
          max-interval: 10000            # Max delay cap: 10s
          multiplier: 2.0                 # Backoff: 1s → 2s → 4s
          max-attempts: 3                 # Total attempts before DLQ
```

### Docker Compose

```yaml
rabbitmq:
  image: rabbitmq:3-management-alpine
  ports:
    - "5672:5672"     # AMQP
    - "15672:15672"   # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
```

---

## Dead Letter Queues (DLQ)

Spring auto-retry handles failures with exponential backoff.
After all attempts are exhausted, the message is sent to the DLQ.

```
Message → Queue → Listener
                    │
                    ├── success → auto ACK → message removed
                    │
                    └── exception thrown → Spring auto-retry
                                             │
                                             ├── attempt 1/3 → wait 1s → retry
                                             ├── attempt 2/3 → wait 2s → retry
                                             └── attempt 3/3 → NACK(requeue=false) → DLX → DLQ (permanent)
```

### Listener Pattern (simple — just throw on failure)

```java
@RabbitListener(queues = QueueConstants.EMAIL_QUEUE)
public void onMessage(String payload) {
    log.info("Email received: {}", payload);
    // If this throws, Spring retries 3 times, then sends to DLQ
    emailService.send(payload);
}
```

### Shared DLQ Listener + Slack Alerts

When a message lands in any DLQ, the shared `DlqListener` picks it up, identifies
the source, and sends a Slack alert automatically.

```
app.email.send.dlq              ─┐
app.notification.single.dlq     ─┤
app.notification.multicast.dlq  ─┼──→ DlqListener → DlqNotificationService
app.notification.topic.dlq      ─┤        ├── Identify source (EMAIL, SINGLE_NOTIFICATION, etc.)
app.audit.event.dlq             ─┘        ├── Send Slack alert (DlqSlackNotifier)
                                           └── Store in DB (TODO)
```

**Slack alert payload:**
```json
{
  "originalQueue": "app.notification.single",
  "messageType": "SINGLE_NOTIFICATION",
  "retryCount": 3,
  "errorReason": "FCM timeout",
  "payload": "{ ... }",
  "failedAt": "2026-02-16T09:10:00Z"
}
```

**Files:**

| File | Responsibility |
|------|----------------|
| `messaging/dlq/DlqListener.java` | Listens to ALL DLQ queues, delegates to service |
| `messaging/dlq/DlqMessage.java` | Unified DLQ payload record |
| `messaging/dlq/DlqNotificationService.java` | Builds alert, identifies source, dispatches Slack + DB |
| `messaging/dlq/DlqSlackNotifier.java` | Sends Slack webhook with formatted alert |

**Config in `application.yaml`:**
```yaml
app:
  dlq:
    slack:
      enabled: true
      webhook-url: https://hooks.slack.com/services/XXX/YYY/ZZZ
```

Or via environment variable:
```bash
export DLQ_SLACK_WEBHOOK_URL=https://hooks.slack.com/services/XXX/YYY/ZZZ
```

### Inspect Failed Messages

1. Open http://localhost:15672 → **Queues**
2. Click on a `.dlq` queue (e.g., `app.email.send.dlq`)
3. Click **"Get Message(s)"** to view the failed payload

---

## Adding a New Domain (e.g. Payment)

Everything stays in one folder — no touching centralized config files for queue setup.

### 1. Add constants in `QueueConstants.java`

```java
public static final String PAYMENT_QUEUE = "app.payment.process";
public static final String PAYMENT_ROUTING_KEY = "app.payment.process";
public static final String PAYMENT_DLQ = "app.payment.process.dlq";
```

### 2. Create domain folder `messaging/payment/`

**PaymentQueueConfig.java** — queue + DLQ + bindings:
```java
@Configuration
public class PaymentQueueConfig {

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(QueueConstants.PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.PAYMENT_DLQ)
                .build();
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(paymentQueue).to(mainExchange).with(QueueConstants.PAYMENT_ROUTING_KEY);
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable(QueueConstants.PAYMENT_DLQ).build();
    }

    @Bean
    public Binding paymentDlqBinding(Queue paymentDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(paymentDlq).to(dlxExchange).with(QueueConstants.PAYMENT_DLQ);
    }
}
```

**PaymentMessage.java** — DTO:
```java
public record PaymentMessage(String orderId, BigDecimal amount, String currency) {}
```

**PaymentPublisher.java** — sends messages:
```java
@Service @RequiredArgsConstructor
public class PaymentPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void send(PaymentMessage message) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.PAYMENT_ROUTING_KEY, message);
    }
}
```

**PaymentListener.java** — consumes messages:
```java
@Component
public class PaymentListener {
    @RabbitListener(queues = QueueConstants.PAYMENT_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        // process + ACK/NACK
    }
}
```

---

## Key URLs

| URL | Description |
|-----|-------------|
| http://localhost:15672 | RabbitMQ Management UI (guest/guest) |
| http://localhost:8082/actuator/health | Health check (includes RabbitMQ status) |

---

## Troubleshooting

### Connection Refused

```
Cannot connect to RabbitMQ on localhost:5672
```

**Fix:** Ensure RabbitMQ is running:
```bash
# Docker
docker-compose up -d rabbitmq

# macOS
brew services start rabbitmq
```

### Messages Stuck in DLQ

1. Check the DLQ in RabbitMQ UI (http://localhost:15672)
2. View the message payload and headers for error details
3. Fix the bug, then re-queue the messages

### Queue Already Exists with Different Arguments

```
PRECONDITION_FAILED - inequivalent arg 'x-dead-letter-exchange'
```

**Fix:** Delete the old queue from the RabbitMQ UI, then restart the app.
