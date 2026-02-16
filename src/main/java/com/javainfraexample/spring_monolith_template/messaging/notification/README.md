# Notification Background Workflow

Complete flow documentation for the notification messaging system — from publishing to delivery,
retry on failure, dead letter queue (DLQ), and Slack alerting.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              NOTIFICATION MESSAGING SYSTEM                            │
│                                                                                      │
│  ┌─────────────┐     ┌──────────────┐     ┌──────────────────────┐                   │
│  │  Business    │────▶│  Notification │────▶│    RabbitMQ           │                   │
│  │  Service     │     │  Publisher    │     │    app.exchange       │                   │
│  └─────────────┘     └──────────────┘     └──────────┬───────────┘                   │
│                                                       │                               │
│                          ┌────────────────────────────┼────────────────────┐          │
│                          │ routing key                │                    │          │
│                          ▼                            ▼                    ▼          │
│               ┌─────────────────┐        ┌──────────────────┐   ┌──────────────┐     │
│               │ app.notification│        │ app.notification  │   │app.notification│   │
│               │ .single         │        │ .multicast        │   │.topic         │    │
│               └────────┬────────┘        └────────┬─────────┘   └──────┬───────┘     │
│                        │                          │                     │              │
│                        ▼                          ▼                     ▼              │
│               ┌─────────────────┐        ┌──────────────────┐   ┌──────────────┐     │
│               │ Single          │        │ Multicast         │   │ Topic        │     │
│               │ Listener        │        │ Listener          │   │ Listener     │     │
│               └────────┬────────┘        └────────┬─────────┘   └──────┬───────┘     │
│                        │                          │                     │              │
│                   ┌────┴────┐                ┌────┴────┐           ┌────┴────┐        │
│                   │ SUCCESS │                │ FAILURE │           │ SUCCESS │        │
│                   └────┬────┘                └────┬────┘           └────┬────┘        │
│                        │                          │                     │              │
│                   Auto ACK                  Spring Retry            Auto ACK          │
│                   Message removed           (see below)            Message removed    │
│                                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Folder Structure

```
messaging/notification/
├── NotificationQueueConfig.java        # 3 queues + 3 DLQs + all bindings
├── publisher/
│   └── NotificationPublisher.java      # Single publisher, routes by type (SINGLE/MULTICAST/TOPIC)
├── listener/
│   ├── SingleNotificationListener.java     # Consumes from app.notification.single
│   ├── MulticastNotificationListener.java  # Consumes from app.notification.multicast
│   └── TopicNotificationListener.java      # Consumes from app.notification.topic
├── message/
│   └── NotificationMessage.java        # Unified DTO record with factory methods
├── constant/
│   └── NotificationConstants.java      # TYPE_SINGLE, TYPE_MULTICAST, TYPE_TOPIC, priorities
└── README.md                           # This file
```

---

## Queue Infrastructure

| Queue | Routing Key | DLQ | Listener |
|-------|-------------|-----|----------|
| `app.notification.single` | `app.notification.single` | `app.notification.single.dlq` | `SingleNotificationListener` |
| `app.notification.multicast` | `app.notification.multicast` | `app.notification.multicast.dlq` | `MulticastNotificationListener` |
| `app.notification.topic` | `app.notification.topic` | `app.notification.topic.dlq` | `TopicNotificationListener` |

**Exchanges:**
- `app.exchange` — main topic exchange (routes messages by routing key)
- `app.exchange.dlx` — dead letter exchange (receives failed messages)

---

## Complete Flow — Success Path

### Step 1: Business Service publishes

```java
// In any service (e.g. OrderService, UserService)
notificationPublisher.sendSingle("device-token-abc", "Order Shipped", "Your order is on the way", "HIGH", Map.of("orderId", "12345"));
```

### Step 2: Publisher routes to correct queue

```
NotificationPublisher.send()
    │
    ├── type = "SINGLE"    → routing key: app.notification.single
    ├── type = "MULTICAST" → routing key: app.notification.multicast
    └── type = "TOPIC"     → routing key: app.notification.topic
    │
    └── rabbitTemplate.convertAndSend("app.exchange", routingKey, message)
```

### Step 3: RabbitMQ routes the message

```
app.exchange (topic exchange)
    │
    ├── routing key matches "app.notification.single"
    │       → delivers to queue: app.notification.single
    │
    └── SingleNotificationListener.onMessage() is called
```

### Step 4: Listener processes successfully

```
SingleNotificationListener.onMessage(payload)
    │
    ├── Deserialize payload → NotificationMessage
    ├── Call business service (e.g. Firebase push)
    ├── No exception thrown
    │
    └── Spring auto-ACKs → message removed from queue ✓
```

**Timeline:**
```
0ms     → Message published to app.exchange
~5ms    → RabbitMQ routes to app.notification.single queue
~10ms   → SingleNotificationListener receives message
~200ms  → Business logic completes (e.g. FCM push sent)
~200ms  → Auto ACK → message permanently removed
```

---

## Complete Flow — Failure + Retry Path

### What triggers retry?

Any **unhandled exception** thrown inside the listener triggers Spring's auto-retry.

```java
@RabbitListener(queues = QueueConstants.NOTIFICATION_SINGLE_QUEUE)
public void onMessage(String payload) {
    // If THIS throws → Spring retries automatically
    notificationService.sendToToken(...);  // throws FCMException("timeout")
}
```

### Retry configuration (application.yaml)

```yaml
spring.rabbitmq.listener.simple.retry:
  enabled: true
  max-attempts: 3           # 3 total attempts (1 original + 2 retries)
  initial-interval: 1000    # 1st retry after 1 second
  max-interval: 10000       # Maximum delay cap: 10 seconds
  multiplier: 2.0           # Exponential backoff multiplier
```

### Retry timeline

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         RETRY FLOW (in-memory)                          │
│                                                                         │
│  T+0ms      Attempt 1/3: onMessage() called                            │
│             → throws FCMException("timeout")                           │
│             → Spring catches exception                                  │
│                                                                         │
│  T+1000ms   Attempt 2/3: onMessage() called again (same message)       │
│             → throws FCMException("timeout")                           │
│             → Spring catches exception                                  │
│                                                                         │
│  T+3000ms   Attempt 3/3: onMessage() called again (1000 × 2.0 = 2s)   │
│             → throws FCMException("timeout")                           │
│             → All retries exhausted                                     │
│                                                                         │
│  T+3000ms   Spring sends NACK(requeue=false)                           │
│             → RabbitMQ reads x-dead-letter-exchange on the queue        │
│             → Routes message to app.exchange.dlx                        │
│             → DLX routes to app.notification.single.dlq                 │
│                                                                         │
└──────────────────────────────────────────────────────────────────────────┘
```

### What happens at each retry attempt

| Attempt | Delay | What happens | Result |
|---------|-------|-------------|--------|
| 1/3 | 0ms (immediate) | `onMessage()` called, exception thrown | Spring catches, waits 1s |
| 2/3 | +1000ms | `onMessage()` called again with same payload | Spring catches, waits 2s |
| 3/3 | +2000ms | `onMessage()` called again with same payload | Exception thrown, all retries exhausted |
| — | +0ms | Spring sends NACK(requeue=false) | Message goes to DLQ |

**Important:** Retry happens **in the same thread, in memory**. The message never leaves the consumer — Spring just calls `onMessage()` again after sleeping.

---

## Complete Flow — DLQ + Slack Alert Path

### After retry exhaustion → DLQ

```
app.notification.single (main queue)
    │
    │  x-dead-letter-exchange: app.exchange.dlx
    │  x-dead-letter-routing-key: app.notification.single.dlq
    │
    └── NACK(requeue=false) → message ejected
            │
            ▼
        app.exchange.dlx (dead letter exchange)
            │
            └── routes by key: app.notification.single.dlq
                    │
                    ▼
                app.notification.single.dlq (dead letter queue)
                    │
                    ▼
                DlqListener.onNotificationSingleDlq()
```

### DLQ processing → Slack alert

```
DlqListener.onNotificationSingleDlq(message)
    │
    └── DlqNotificationService.process(message, dlqName)
            │
            ├── 1. Extract from x-death headers:
            │       • originalQueue: "app.notification.single"
            │       • retryCount: 3
            │       • errorReason: "FCM timeout"
            │
            ├── 2. Identify type: "SINGLE_NOTIFICATION"
            │
            ├── 3. Build DlqMessage record:
            │       {
            │         "originalQueue": "app.notification.single",
            │         "messageType": "SINGLE_NOTIFICATION",
            │         "retryCount": 3,
            │         "errorReason": "FCM timeout",
            │         "payload": "{\"type\":\"SINGLE\",\"token\":\"abc\",...}",
            │         "failedAt": "2026-02-11T09:10:00Z"
            │       }
            │
            ├── 4. DlqSlackNotifier.notify(dlqMessage)
            │       → POST to Slack webhook URL
            │       → Formatted Block Kit message with all details
            │
            └── 5. Store in DB (TODO — commented for now)
                    // dlqRecordRepository.save(DlqRecord.from(dlqMessage))
```

### Slack alert received

```
🚨 DLQ Alert — Message Failed

Queue:      app.notification.single
Type:       SINGLE_NOTIFICATION
Retries:    3
Failed At:  2026-02-11T09:10:00Z

Error:
  FCM timeout

Payload:
  {"type":"SINGLE","token":"device-token-abc","title":"Order Shipped",...}
```

---

## Complete Timeline (end-to-end failure scenario)

```
T+0ms       orderService.shipOrder() → notificationPublisher.sendSingle(...)
T+5ms       Message arrives in app.notification.single queue
T+10ms      SingleNotificationListener.onMessage() — Attempt 1/3
T+10ms      → FCMException("timeout") thrown
T+1010ms    SingleNotificationListener.onMessage() — Attempt 2/3 (waited 1s)
T+1010ms    → FCMException("timeout") thrown
T+3010ms    SingleNotificationListener.onMessage() — Attempt 3/3 (waited 2s)
T+3010ms    → FCMException("timeout") thrown — ALL RETRIES EXHAUSTED
T+3010ms    Spring sends NACK(requeue=false)
T+3015ms    RabbitMQ routes to app.exchange.dlx → app.notification.single.dlq
T+3020ms    DlqListener picks up message from DLQ
T+3020ms    DlqNotificationService identifies: SINGLE_NOTIFICATION, retries=3
T+3050ms    DlqSlackNotifier posts to Slack webhook
T+3100ms    ✓ Team receives Slack alert with full failure details
```

---

## Message Payload (NotificationMessage)

```java
// Single — one device/user
NotificationMessage.single(token, title, body, priority, data);

// Multicast — list of devices/users
NotificationMessage.multicast(tokens, title, body, priority, data);

// Topic — all subscribers
NotificationMessage.topic(topic, title, body, priority, data);
```

### Fields

| Field | Type | Used By | Description |
|-------|------|---------|-------------|
| `type` | String | All | `SINGLE`, `MULTICAST`, `TOPIC` — determines routing |
| `token` | String | Single | Target device/user token |
| `tokens` | List\<String\> | Multicast | List of target tokens |
| `topic` | String | Topic | Topic name for broadcast |
| `title` | String | All | Notification title |
| `body` | String | All | Notification body text |
| `priority` | String | All | `HIGH`, `NORMAL`, `LOW` |
| `data` | Map | All | Extra payload (deep link, image URL, etc.) |

---

## Publisher API

```java
// Inject
private final NotificationPublisher notificationPublisher;

// Single
notificationPublisher.sendSingle("token-123", "Welcome!", "Hello", "HIGH", Map.of("url", "/home"));

// Multicast
notificationPublisher.sendMulticast(List.of("t1", "t2"), "Update", "New version", "NORMAL", Map.of());

// Topic
notificationPublisher.sendTopic("news", "Breaking", "Big update", "LOW", Map.of("category", "tech"));

// Generic (auto-routes by type)
notificationPublisher.send(NotificationMessage.single(...));
```

---

## Configuration Reference

### application.yaml

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: auto          # Spring manages ACK/NACK
        prefetch: 1                     # Fair dispatch
        concurrency: 1                  # Start with 1 consumer
        max-concurrency: 3              # Scale up to 3 under load
        retry:
          enabled: true
          max-attempts: 3               # 3 total attempts
          initial-interval: 1000        # 1st retry: 1s
          max-interval: 10000          # Max cap: 10s
          multiplier: 2.0               # Backoff: 1s → 2s → 4s

app:
  dlq:
    slack:
      enabled: true                     # Enable/disable Slack alerts
      webhook-url: ${DLQ_SLACK_WEBHOOK_URL:}
```

---

## Troubleshooting

### Messages not being consumed

1. Check RabbitMQ UI (http://localhost:15672) → Queues → verify messages are in the queue
2. Check application logs for listener startup: `Registered listener for queue: app.notification.single`
3. Verify RabbitMQ connection in `application.yaml`

### Messages going to DLQ immediately (no retry)

1. Verify `retry.enabled: true` in `application.yaml`
2. Verify `acknowledge-mode: auto` (retry doesn't work with `manual`)
3. Check if the exception is a `MessageConversionException` (these are not retried)

### DLQ messages not triggering Slack

1. Verify `app.dlq.slack.enabled: true`
2. Verify webhook URL is set: `DLQ_SLACK_WEBHOOK_URL` env variable
3. Check logs for: `DLQ processing itself failed`

### Inspect failed messages manually

1. Open http://localhost:15672 → **Queues**
2. Click on `app.notification.single.dlq`
3. Click **"Get Message(s)"** to view payload and headers
