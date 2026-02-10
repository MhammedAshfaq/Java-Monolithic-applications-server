# Quartz Scheduler (DB-Backed Cron Jobs)

## Overview

Quartz is a **production-grade job scheduler** that stores job state in PostgreSQL. Unlike Spring `@Scheduled` (in-memory), Quartz jobs **survive application restarts**, support **clustering** (multiple instances), and handle **misfires** (missed schedules).

```
┌─────────────────────────────────────────────────────┐
│                    Application                       │
│                                                      │
│  ┌──────────────┐    ┌──────────────┐               │
│  │ QuartzConfig │    │ Quartz Jobs  │               │
│  │ (triggers)   │───▶│ (thin logic) │──▶ RabbitMQ   │
│  └──────┬───────┘    └──────────────┘    (heavy work)│
│         │                                            │
│  ┌──────▼───────────────────────────────┐           │
│  │           Quartz Engine              │           │
│  │  • Thread pool (5 threads)           │           │
│  │  • Misfire detection                 │           │
│  │  • Cluster lock management           │           │
│  └──────┬───────────────────────────────┘           │
│         │                                            │
└─────────┼────────────────────────────────────────────┘
          │ JDBC
┌─────────▼───────────────────────────────────────────┐
│              PostgreSQL (QRTZ_ tables)               │
│  Job definitions, triggers, fire history, locks      │
└─────────────────────────────────────────────────────┘
```

## Folder Structure

```
config/
  quartz/
    QuartzConfig.java              ← Registers jobs + triggers (WHEN to fire)
  scheduler/
    SchedulerConfig.java           ← Spring @Scheduled thread pool (polling only)

scheduler/
  SampleScheduledTasks.java        ← Lightweight polling (in-memory, non-critical)
  jobs/
    DailyCleanupJob.java           ← Quartz: every midnight
    WeeklyReportJob.java           ← Quartz: every Monday at midnight
    HealthCheckJob.java            ← Quartz: every 3 minutes (observable)
```

## When to Use Quartz vs @Scheduled

| Feature | Spring `@Scheduled` | Quartz (DB-backed) |
|---------|---------------------|---------------------|
| **Use for** | Polling, heartbeats | Business-critical crons |
| **Storage** | In-memory | PostgreSQL (QRTZ_ tables) |
| **Survives restart?** | No | Yes |
| **Clustering?** | No | Yes (`isClustered: true`) |
| **Misfire handling?** | No | Yes (FireAndProceed) |
| **Example** | pollingTask (5 min) | DailyCleanupJob, HealthCheckJob |

**Rule of thumb**: If the job is business-critical and must not be missed, use Quartz. If it's a simple polling task, use `@Scheduled`.

---

## How It Works: Execution Flow

### 1. Application Startup

When the app starts, Spring Boot + Quartz does the following:

```
App starts
  │
  ▼
QuartzAutoConfiguration creates SchedulerFactoryBean
  │
  ▼
Connects to PostgreSQL (QRTZ_ tables)
  │
  ▼
Reads QuartzConfig.java → finds @Bean JobDetail + Trigger
  │
  ▼
INSERT/UPDATE into QRTZ_JOB_DETAILS (job definition)
INSERT/UPDATE into QRTZ_TRIGGERS (trigger schedule)
INSERT/UPDATE into QRTZ_CRON_TRIGGERS (cron expression)
  │
  ▼
Scheduler starts → enters cluster, acquires lock
  │
  ▼
INSERT into QRTZ_SCHEDULER_STATE (this instance registered)
  │
  ▼
Ready! Waiting for next fire time...
```

### 2. When a Trigger Fires (e.g., every 3 minutes)

```
Quartz thread pool checks QRTZ_TRIGGERS.NEXT_FIRE_TIME
  │
  ▼
Time reached? → Acquire lock in QRTZ_LOCKS (cluster-safe)
  │
  ▼
INSERT into QRTZ_FIRED_TRIGGERS (state=EXECUTING)
  │
  ▼
UPDATE QRTZ_TRIGGERS:
  - TRIGGER_STATE: WAITING → ACQUIRED → EXECUTING
  - PREV_FIRE_TIME = current time
  - NEXT_FIRE_TIME = calculated next fire time
  │
  ▼
Execute HealthCheckJob.executeInternal()
  │
  ▼
Job completes successfully
  │
  ▼
DELETE from QRTZ_FIRED_TRIGGERS (execution record removed)
UPDATE QRTZ_TRIGGERS: TRIGGER_STATE = WAITING
  │
  ▼
Back to waiting for next fire time...
```

### 3. If the App Crashes Mid-Execution

```
Job is executing → app crashes
  │
  ▼
QRTZ_FIRED_TRIGGERS still has row with state=EXECUTING
  │
  ▼
App restarts → Quartz checks QRTZ_FIRED_TRIGGERS
  │
  ▼
Finds orphaned EXECUTING row + requestRecovery=true
  │
  ▼
Re-executes the job automatically
```

### 4. If the App Was Down at Scheduled Time (Misfire)

```
Job was scheduled at 00:00 → app was down
  │
  ▼
App restarts at 02:00
  │
  ▼
Quartz checks: NEXT_FIRE_TIME < now AND misfireThreshold exceeded
  │
  ▼
withMisfireHandlingInstructionFireAndProceed()
  │
  ▼
Fires the missed job IMMEDIATELY, then resumes normal schedule
```

---

## PostgreSQL Tables (QRTZ_) Explained

All tables are created by Flyway migration: `20260209180000__create_quartz_tables.sql`

### Core Tables

#### `QRTZ_JOB_DETAILS` — What jobs exist

Stores the **definition** of each registered job. One row per job.

| Column | Description | Example |
|--------|-------------|---------|
| `SCHED_NAME` | Scheduler instance name | `spring-scheduler` |
| `JOB_NAME` | Unique job identifier | `healthCheckJob` |
| `JOB_GROUP` | Logical grouping | `scheduled-jobs` |
| `JOB_CLASS_NAME` | Java class to execute | `...scheduler.jobs.HealthCheckJob` |
| `IS_DURABLE` | Keep job even if no triggers | `true` (storeDurably) |
| `IS_NONCONCURRENT` | Prevent parallel execution | `false` |
| `REQUESTS_RECOVERY` | Re-execute after crash | `true` (requestRecovery) |
| `JOB_DATA` | Serialized JobDataMap (BYTEA) | Binary blob |

**When created**: On first app startup (or when `overwrite-existing-jobs: true` replaces existing).

```sql
-- View all registered jobs
SELECT job_name, job_group, job_class_name, is_durable, requests_recovery
FROM qrtz_job_details;
```

#### `QRTZ_TRIGGERS` — When jobs should fire

Stores **trigger schedules**. One row per trigger. This is the most actively updated table.

| Column | Description | Example |
|--------|-------------|---------|
| `TRIGGER_NAME` | Unique trigger ID | `healthCheckTrigger` |
| `TRIGGER_GROUP` | Logical grouping | `scheduled-triggers` |
| `JOB_NAME` | Links to QRTZ_JOB_DETAILS | `healthCheckJob` |
| `NEXT_FIRE_TIME` | Next execution (epoch ms) | `1770661380000` |
| `PREV_FIRE_TIME` | Last execution (epoch ms) | `1770661200000` |
| `TRIGGER_STATE` | Current state | `WAITING` / `ACQUIRED` / `EXECUTING` |
| `TRIGGER_TYPE` | Type of trigger | `CRON` |
| `MISFIRE_INSTR` | Misfire strategy | `1` (FireAndProceed) |

**When updated**: Every time a trigger fires — `NEXT_FIRE_TIME`, `PREV_FIRE_TIME`, and `TRIGGER_STATE` are updated.

```sql
-- View all triggers with next/prev fire times (human-readable)
SELECT
  trigger_name,
  trigger_group,
  job_name,
  trigger_state,
  trigger_type,
  TO_TIMESTAMP(next_fire_time / 1000) AS next_fire,
  TO_TIMESTAMP(prev_fire_time / 1000) AS prev_fire
FROM qrtz_triggers;
```

**Trigger state lifecycle per execution**:
```
WAITING → ACQUIRED → EXECUTING → WAITING
                                    ↑
                              (cycle repeats)
```

#### `QRTZ_CRON_TRIGGERS` — Cron expressions

Stores the cron expression for each cron-type trigger.

| Column | Description | Example |
|--------|-------------|---------|
| `TRIGGER_NAME` | Links to QRTZ_TRIGGERS | `healthCheckTrigger` |
| `CRON_EXPRESSION` | The cron schedule | `0 */3 * * * ?` |
| `TIME_ZONE_ID` | Timezone for evaluation | `UTC` |

**When created**: On app startup, alongside the trigger.

```sql
-- View all cron schedules
SELECT t.trigger_name, t.job_name, c.cron_expression, t.trigger_state
FROM qrtz_triggers t
JOIN qrtz_cron_triggers c
  ON t.sched_name = c.sched_name
  AND t.trigger_name = c.trigger_name
  AND t.trigger_group = c.trigger_group;
```

### Runtime Tables

#### `QRTZ_FIRED_TRIGGERS` — Currently executing jobs

Stores a row for **each job that is currently executing**. Rows are inserted when a trigger fires and deleted when the job completes.

| Column | Description | Example |
|--------|-------------|---------|
| `ENTRY_ID` | Unique fire instance ID | `4bb732fc8dfc...` |
| `TRIGGER_NAME` | Which trigger fired | `healthCheckTrigger` |
| `INSTANCE_NAME` | Which app instance ran it | `4bb732fc8dfc177...` |
| `FIRED_TIME` | When it started (epoch ms) | `1770661200000` |
| `STATE` | Execution state | `EXECUTING` |

**When created**: When a trigger fires (job starts).
**When deleted**: When the job completes successfully.
**When orphaned**: If the app crashes mid-execution (Quartz detects this on restart).

```sql
-- View currently executing jobs (empty if no jobs running right now)
SELECT
  trigger_name,
  job_name,
  instance_name,
  state,
  TO_TIMESTAMP(fired_time / 1000) AS fired_at
FROM qrtz_fired_triggers;
```

#### `QRTZ_SCHEDULER_STATE` — Cluster node registry

Tracks all running application instances in the cluster.

| Column | Description | Example |
|--------|-------------|---------|
| `INSTANCE_NAME` | Unique instance ID | `4bb732fc8dfc177...` |
| `LAST_CHECKIN_TIME` | Last heartbeat (epoch ms) | `1770661380000` |
| `CHECKIN_INTERVAL` | How often it checks in (ms) | `15000` (15 seconds) |

**When updated**: Every `clusterCheckinInterval` (15s). This is how Quartz detects dead nodes.

```sql
-- View active scheduler instances
SELECT
  instance_name,
  TO_TIMESTAMP(last_checkin_time / 1000) AS last_checkin,
  checkin_interval
FROM qrtz_scheduler_state;
```

#### `QRTZ_LOCKS` — Cluster synchronization

Used for **row-level locking** so only one instance can acquire a trigger at a time.

| Column | Description | Example |
|--------|-------------|---------|
| `LOCK_NAME` | Lock type | `TRIGGER_ACCESS` / `STATE_ACCESS` |

```sql
-- View locks (always present, used for synchronization)
SELECT * FROM qrtz_locks;
```

### Other Tables

| Table | Purpose |
|-------|---------|
| `QRTZ_SIMPLE_TRIGGERS` | For simple interval triggers (not cron) |
| `QRTZ_SIMPROP_TRIGGERS` | For triggers with custom properties |
| `QRTZ_BLOB_TRIGGERS` | For triggers stored as binary blobs |
| `QRTZ_CALENDARS` | For excluding dates (e.g., holidays) |
| `QRTZ_PAUSED_TRIGGER_GRPS` | Tracks paused trigger groups |

---

## Observing the 3-Minute HealthCheckJob

The `HealthCheckJob` runs every 3 minutes so you can observe the DB flow in real-time.

### Step 1: Watch the logs

```bash
docker logs -f spring-app 2>&1 | grep "Quartz"
```

You'll see every 3 minutes:
```
[Quartz] HealthCheckJob STARTED | fireId=... | trigger=scheduled-triggers.healthCheckTrigger | time=...
[Quartz] Running system health check...
[Quartz] HealthCheckJob COMPLETED | fireId=... | duration=~500ms
```

### Step 2: Query the trigger state

```bash
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "
SELECT
  trigger_name,
  job_name,
  trigger_state,
  TO_TIMESTAMP(next_fire_time / 1000) AS next_fire,
  TO_TIMESTAMP(prev_fire_time / 1000) AS prev_fire
FROM qrtz_triggers
ORDER BY trigger_name;
"
```

Expected output:
```
     trigger_name      |     job_name     | trigger_state |       next_fire        |       prev_fire
-----------------------+------------------+---------------+------------------------+------------------------
 dailyCleanupTrigger   | dailyCleanupJob  | WAITING       | 2026-02-10 00:00:00+00 | -infinity
 healthCheckTrigger    | healthCheckJob   | WAITING       | 2026-02-09 18:15:00+00 | 2026-02-09 18:12:00+00
 weeklyReportTrigger   | weeklyReportJob  | WAITING       | 2026-02-16 00:00:00+00 | -infinity
```

### Step 3: Query cron expressions

```bash
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "
SELECT t.trigger_name, t.job_name, c.cron_expression, t.trigger_state
FROM qrtz_triggers t
JOIN qrtz_cron_triggers c
  ON t.sched_name = c.sched_name
  AND t.trigger_name = c.trigger_name
  AND t.trigger_group = c.trigger_group
ORDER BY t.trigger_name;
"
```

### Step 4: Query the scheduler state

```bash
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "
SELECT
  instance_name,
  TO_TIMESTAMP(last_checkin_time / 1000) AS last_checkin,
  checkin_interval AS checkin_ms
FROM qrtz_scheduler_state;
"
```

### Step 5: Catch a running job (query FAST while job is executing)

```bash
docker exec spring-postgres psql -U postgres -d java-spring-mololithic -c "
SELECT
  trigger_name,
  job_name,
  state,
  TO_TIMESTAMP(fired_time / 1000) AS fired_at
FROM qrtz_fired_triggers;
"
```

This table is usually **empty** (jobs complete in ~500ms). You'd need to query at exactly the right moment or add a `Thread.sleep()` to the job to catch it.

---

## Configuration Reference

### application.yaml

```yaml
spring:
  quartz:
    job-store-type: jdbc                    # DB-backed (not in-memory)
    jdbc:
      initialize-schema: never              # Flyway manages tables
    overwrite-existing-jobs: true           # Replace jobs on restart
    properties:
      org:
        quartz:
          scheduler:
            instanceName: spring-scheduler  # Scheduler name
            instanceId: AUTO                # Unique per instance
          jobStore:
            driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
            tablePrefix: QRTZ_
            isClustered: true               # Multi-instance safe
            clusterCheckinInterval: 15000   # 15s heartbeat
            misfireThreshold: 60000         # 60s before misfire
          threadPool:
            threadCount: 5                  # Max concurrent jobs
            threadPriority: 5
```

### Key configuration notes

| Property | Why it matters |
|----------|---------------|
| `job-store-type: jdbc` | Stores in DB, not memory |
| `initialize-schema: never` | Flyway migration handles QRTZ_ tables |
| `overwrite-existing-jobs: true` | Avoids "job already exists" errors on restart |
| `driverDelegateClass: PostgreSQLDelegate` | **REQUIRED** for PostgreSQL — uses `getBytes()` instead of `getBlob()` |
| `isClustered: true` | Only one instance runs each trigger |
| `instanceId: AUTO` | Each app instance gets a unique ID |

---

## Adding a New Quartz Job

### Step 1: Create the Job class

```java
// scheduler/jobs/MyNewJob.java
@Slf4j
@Component
@RequiredArgsConstructor
public class MyNewJob extends QuartzJobBean {

    // Inject any service (Spring DI works in Quartz jobs)
    // private final MyService myService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[Quartz] MyNewJob started");

        try {
            // Keep it THIN — dispatch heavy work to RabbitMQ
            // jobProducer.sendJob(JobType.GENERAL, "my-task", Map.of("key", "value"));

            log.info("[Quartz] MyNewJob completed");
        } catch (Exception e) {
            log.error("[Quartz] MyNewJob failed: {}", e.getMessage(), e);
            throw new JobExecutionException("MyNewJob failed", e);
        }
    }
}
```

### Step 2: Register Job + Trigger in QuartzConfig

```java
// config/quartz/QuartzConfig.java

@Bean
public JobDetail myNewJobDetail() {
    return JobBuilder.newJob(MyNewJob.class)
            .withIdentity("myNewJob", "scheduled-jobs")
            .withDescription("My new scheduled job")
            .storeDurably()
            .requestRecovery(true)
            .build();
}

@Bean
public Trigger myNewTrigger() {
    return TriggerBuilder.newTrigger()
            .forJob(myNewJobDetail())
            .withIdentity("myNewTrigger", "scheduled-triggers")
            .withSchedule(
                CronScheduleBuilder
                    .cronSchedule("0 0 9 * * ?")  // every day at 9 AM
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .build();
}
```

### Common Cron Expressions

| Expression | Schedule |
|------------|----------|
| `0 0 0 * * ?` | Every midnight |
| `0 0 0 ? * MON` | Every Monday at midnight |
| `0 */3 * * * ?` | Every 3 minutes |
| `0 0 9 * * ?` | Every day at 9 AM |
| `0 30 9 ? * MON-FRI` | Weekdays at 9:30 AM |
| `0 0 */2 * * ?` | Every 2 hours |
| `0 0 8,20 * * ?` | At 8 AM and 8 PM daily |

### Cron format

```
┌───────── second (0-59)
│ ┌─────── minute (0-59)
│ │ ┌───── hour (0-23)
│ │ │ ┌─── day of month (1-31)
│ │ │ │ ┌─ month (1-12)
│ │ │ │ │ ┌── day of week (1-7 or SUN-SAT)
│ │ │ │ │ │
0 0 0 * * ?    ← every midnight
```

---

## Production Rules

1. **Keep Quartz jobs THIN** — Don't put business logic in the job. Dispatch to RabbitMQ.
2. **Always set `requestRecovery(true)`** — So crashed jobs get re-executed.
3. **Always use `withMisfireHandlingInstructionFireAndProceed()`** — So missed jobs run when the app comes back.
4. **Use `storeDurably()`** — So the job definition persists even if triggers are temporarily removed.
5. **Never use `initialize-schema: always`** in production — Use Flyway to manage QRTZ_ tables.
6. **Monitor `QRTZ_FIRED_TRIGGERS`** — If rows accumulate, jobs are stuck or taking too long.
