# Login API — Complete Workflow Diagram

End-to-end flow for `POST /auth/login` including the background email job.

---

## High-Level Overview

```
┌──────────┐       ┌────────────────┐       ┌─────────────┐       ┌──────────────┐
│  Client   │──────▶│ AuthController │──────▶│ AuthService  │──────▶│   Response   │
│ (Postman) │       │ POST /auth/    │       │   login()    │       │  200 OK      │
└──────────┘       │    login       │       └──────┬───────┘       └──────────────┘
                   └────────────────┘              │
                                                   │ fire & forget (async)
                                                   ▼
                                          ┌──────────────────┐
                                          │  EmailPublisher   │
                                          │  sendLogin()      │
                                          └────────┬─────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │   RabbitMQ        │
                                          │   app.email.send  │
                                          └────────┬─────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │  EmailListener    │
                                          │  onMessage()      │
                                          └────────┬─────────┘
                                                   │
                                                   ▼
                                          ┌──────────────────┐
                                          │  EmailService     │
                                          │  process()        │
                                          │  → handleLogin()  │
                                          └──────────────────┘
```

---

## Detailed Step-by-Step Flow

```
 CLIENT                     SPRING BOOT                          RABBITMQ                    BACKGROUND WORKER
═══════                    ════════════                         ══════════                   ══════════════════

   │                            │                                   │                              │
   │  POST /auth/login          │                                   │                              │
   │  {                         │                                   │                              │
   │    "email": "a@b.com",     │                                   │                              │
   │    "password": "123456"    │                                   │                              │
   │  }                         │                                   │                              │
   │ ──────────────────────────▶│                                   │                              │
   │                            │                                   │                              │
   │                     ┌──────┴──────┐                            │                              │
   │                     │ AuthController                           │                              │
   │                     │ @PostMapping │                           │                              │
   │                     │ ("/login")   │                           │                              │
   │                     └──────┬──────┘                            │                              │
   │                            │                                   │                              │
   │                            │  @Valid LoginRequest              │                              │
   │                            │  ├── email: @NotBlank @Email      │                              │
   │                            │  └── password: @NotBlank @Size(6) │                              │
   │                            │                                   │                              │
   │                     ┌──────┴──────┐                            │                              │
   │                     │ AuthService  │                           │                              │
   │                     │  login()     │                           │                              │
   │                     └──────┬──────┘                            │                              │
   │                            │                                   │                              │
   │                            │  1. Authenticate user             │                              │
   │                            │     (TODO: DB lookup,             │                              │
   │                            │      password verify, JWT)        │                              │
   │                            │                                   │                              │
   │                            │  2. Build LoginResponse           │                              │
   │                            │     ├── accessToken               │                              │
   │                            │     ├── refreshToken              │                              │
   │                            │     ├── tokenType: "Bearer"       │                              │
   │                            │     └── expiresIn: 3600           │                              │
   │                            │                                   │                              │
   │                            │  3. Fire background email ────────┼──────────────────────────────│
   │                            │     emailPublisher.sendLogin()    │                              │
   │                            │         │                         │                              │
   │                            │         │  EmailMessage.login()   │                              │
   │                            │         │  ├── type: "LOGIN"      │                              │
   │                            │         │  ├── to: "a@b.com"      │                              │
   │                            │         │  ├── name: "John"       │                              │
   │                            │         │  ├── subject: "Login Notification"                     │
   │                            │         │  ├── body: "Hi John..." │                              │
   │                            │         │  └── data: {}           │                              │
   │                            │         │                         │                              │
   │                            │         ▼                         │                              │
   │                            │  rabbitTemplate                   │                              │
   │                            │  .convertAndSend(                 │                              │
   │                            │     "app.exchange",           ───▶│  app.exchange                │
   │                            │     "app.email.send",             │    │                         │
   │                            │     emailMessage)                 │    │ route by key             │
   │                            │                                   │    ▼                         │
   │                            │  4. Return response               │  ┌──────────────────┐       │
   │ ◀─────────────────────────│     immediately                   │  │ app.email.send    │       │
   │                            │     (non-blocking)                │  │ (durable queue)   │       │
   │  200 OK                    │                                   │  └────────┬─────────┘       │
   │  {                         │                                   │           │                  │
   │    "success": true,        │                                   │           │  deliver         │
   │    "message": "Login       │                                   │           │                  │
   │      successful",          │                                   │           ▼                  │
   │    "data": {               │                                   │  ┌──────────────────┐       │
   │      "accessToken": "...", │                                   │  │ EmailListener     │──────▶│
   │      "refreshToken": "...",│                                   │  │ onMessage(        │       │
   │      "tokenType": "Bearer",│                                  │  │  EmailMessage)    │       │
   │      "expiresIn": 3600     │                                   │  └──────────────────┘       │
   │    }                       │                                   │                              │
   │  }                         │                                   │                     ┌────────┴────────┐
   │                            │                                   │                     │  EmailService    │
   │                            │                                   │                     │  process()       │
   │                            │                                   │                     │                  │
   │                            │                                   │                     │  switch(type) {  │
   │                            │                                   │                     │   WELCOME → ...  │
   │                            │                                   │                     │   LOGIN → ✓      │
   │                            │                                   │                     │   USER_UPDATE →  │
   │                            │                                   │                     │   ...            │
   │                            │                                   │                     │  }               │
   │                            │                                   │                     │                  │
   │                            │                                   │                     │  handleLogin()   │
   │                            │                                   │                     │  → Send email    │
   │                            │                                   │                     └────────┬────────┘
   │                            │                                   │                              │
   │                            │                                   │     ◀── Auto ACK ───────────│
   │                            │                                   │     message removed          │
   │                            │                                   │                              │
```

---

## Failure + Retry + DLQ Flow

```
   RABBITMQ                              BACKGROUND WORKER                          DLQ FLOW
  ══════════                            ══════════════════                         ══════════

  ┌──────────────────┐
  │ app.email.send    │
  │ (message waiting) │
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │ EmailListener     │
  │ onMessage()       │──────▶ EmailService.process()
  └──────────────────┘                │
                                      │
                              ┌───────┴────────┐
                              │  handleLogin()  │
                              │                 │
                              │  SMTP fails!    │
                              │  throws Exception│
                              └───────┬────────┘
                                      │
                    ┌─────────────────┴─────────────────────┐
                    │         SPRING AUTO-RETRY              │
                    │                                        │
                    │  Attempt 1/3 ─── T+0ms ─── FAILED     │
                    │       │                                │
                    │       ▼  wait 1s (initial-interval)    │
                    │                                        │
                    │  Attempt 2/3 ─── T+1s ──── FAILED     │
                    │       │                                │
                    │       ▼  wait 2s (1s × 2.0 multiplier) │
                    │                                        │
                    │  Attempt 3/3 ─── T+3s ──── FAILED     │
                    │       │                                │
                    │       ▼  ALL RETRIES EXHAUSTED         │
                    └─────────────────┬─────────────────────┘
                                      │
                                      │  NACK (requeue=false)
                                      ▼
                    ┌──────────────────────────────────┐
                    │  Queue arg: x-dead-letter-exchange│
                    │  → app.exchange.dlx               │
                    │  Queue arg: x-dead-letter-routing │
                    │  → app.email.send.dlq             │
                    └────────────────┬─────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────┐
                    │  app.exchange.dlx (DLX exchange)  │
                    │  routes → app.email.send.dlq      │
                    └────────────────┬─────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────┐
                    │  app.email.send.dlq               │
                    │  (dead letter queue)               │
                    └────────────────┬─────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────┐
                    │  DlqListener                      │
                    │  onEmailDlq(message)              │
                    └────────────────┬─────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────┐
                    │  DlqNotificationService           │
                    │  .process(message, dlqName)       │
                    │                                    │
                    │  Builds DlqMessage:                │
                    │  {                                  │
                    │    "originalQueue": "app.email.send"│
                    │    "messageType": "EMAIL",          │
                    │    "retryCount": 3,                 │
                    │    "errorReason": "SMTP timeout",   │
                    │    "payload": "{...}",              │
                    │    "failedAt": "2026-02-17T..."     │
                    │  }                                  │
                    └────────────────┬─────────────────┘
                                     │
                                     ▼
                    ┌──────────────────────────────────┐
                    │  DlqSlackNotifier                 │
                    │  .notify(dlqMessage)              │
                    │                                    │
                    │  POST → Slack Webhook              │
                    │                                    │
                    │  🚨 DLQ Alert — Message Failed     │
                    │  Queue: app.email.send              │
                    │  Type:  EMAIL                       │
                    │  Error: SMTP timeout                │
                    └──────────────────────────────────┘
```

---

## File Flow Map

Shows exactly which files are involved at each step:

```
REQUEST PHASE (synchronous — client waits)
════════════════════════════════════════════

  api/auth/dto/LoginRequest.java          DTO: { email, password }
       │
       ▼
  api/auth/AuthController.java            @PostMapping("/login")
       │
       ▼
  services/auth/AuthService.java          login() → builds response + fires email
       │
       ├── api/auth/dto/LoginResponse.java    DTO: { accessToken, refreshToken, tokenType, expiresIn }
       │
       └── common/dto/ApiResponseDto.java     Wrapper: { success, message, data, timestamp }


PUBLISH PHASE (fire & forget — non-blocking)
═════════════════════════════════════════════

  messaging/email/publisher/EmailPublisher.java    sendLogin() → rabbitTemplate.convertAndSend()
       │
       └── messaging/email/message/EmailMessage.java   record: { type, to, name, subject, body, data }
                │
                └── messaging/email/constant/EmailConstants.java   TYPE_LOGIN = "LOGIN"


QUEUE INFRASTRUCTURE (RabbitMQ)
════════════════════════════════

  config/rabbitmq/RabbitMQConfig.java              Shared: exchange, DLX, message converter
       │
       └── messaging/email/EmailQueueConfig.java   Queue: app.email.send + DLQ + bindings
                │
                └── messaging/constant/QueueConstants.java   Queue/routing key names


CONSUME PHASE (asynchronous — background)
══════════════════════════════════════════

  messaging/email/listener/EmailListener.java      @RabbitListener → onMessage(EmailMessage)
       │
       ▼
  services/email/EmailService.java                 process() → switch(type) → handleLogin()


DLQ PHASE (on failure after 3 retries)
═══════════════════════════════════════

  messaging/dlq/DlqListener.java                   @RabbitListener(app.email.send.dlq)
       │
       ▼
  messaging/dlq/DlqNotificationService.java        Builds DlqMessage, identifies source
       │
       ▼
  messaging/dlq/DlqSlackNotifier.java              POST → Slack webhook
```

---

## Timeline

### Success Scenario

```
T+0ms       Client sends POST /auth/login { "email": "a@b.com", "password": "123456" }
T+5ms       AuthController validates @Valid LoginRequest (email format, password length)
T+10ms      AuthService.login() — authenticates user
T+15ms      AuthService calls emailPublisher.sendLogin("a@b.com", "John", {})
T+20ms      EmailPublisher → rabbitTemplate.convertAndSend() → message in queue
T+20ms      AuthService returns LoginResponse (client gets 200 OK immediately)
  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
T+25ms      EmailListener picks up message from app.email.send
T+25ms      Spring auto-deserializes JSON → EmailMessage (via JacksonJsonMessageConverter)
T+30ms      EmailService.process() → switch("LOGIN") → handleLogin()
T+200ms     Email sent via SMTP
T+200ms     Auto ACK → message permanently removed from queue ✓
```

### Failure Scenario

```
T+0ms       Client sends POST /auth/login
T+20ms      Client gets 200 OK (background email already queued)
  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
T+25ms      EmailListener picks up message
T+30ms      EmailService.handleLogin() → SMTP throws exception ✗
T+1030ms    RETRY 2/3 → handleLogin() → fails again ✗
T+3030ms    RETRY 3/3 → handleLogin() → fails again ✗
T+3030ms    All retries exhausted → NACK(requeue=false)
T+3035ms    RabbitMQ routes to app.email.send.dlq
T+3040ms    DlqListener picks up from DLQ
T+3045ms    DlqNotificationService builds alert payload
T+3050ms    DlqSlackNotifier → POST to Slack webhook
T+3100ms    Team gets Slack alert with full failure details 🚨
```

---

## cURL Test Command

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'
```

### Expected Response

```json
{
  "timestamp": "2026-02-17T17:38:44",
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "access_token_placeholder",
    "refreshToken": "refresh_token_placeholder",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### Expected Logs (Success)

```
INFO  AuthService          : Login attempt for email: test@example.com
INFO  EmailPublisher       : Email published: type=LOGIN, to=test@example.com, subject=Login Notification
INFO  EmailListener        : [EMAIL QUEUE] Message received: type=LOGIN, to=test@example.com
INFO  EmailService         : [EMAIL SERVICE] Processing email: type=LOGIN, to=test@example.com
INFO  EmailService         : [EMAIL] Sending login notification to: test@example.com
INFO  EmailService         : [EMAIL] Login notification sent to: test@example.com (name=test@example.com)
INFO  EmailService         : [EMAIL SERVICE] Email sent successfully: type=LOGIN, to=test@example.com
INFO  EmailListener        : [EMAIL QUEUE] Completed: type=LOGIN, to=test@example.com
```
