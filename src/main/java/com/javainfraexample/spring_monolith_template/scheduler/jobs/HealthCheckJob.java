package com.javainfraexample.spring_monolith_template.scheduler.jobs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Job: Runs every 3 minutes (DB-backed, observable).
 * 
 * Purpose: Demonstrates the Quartz execution flow with PostgreSQL.
 * You can query QRTZ_ tables to see how Quartz stores and updates
 * job state, trigger state, fire times, and scheduler state.
 * 
 * DB tables updated during each execution cycle:
 *   1. QRTZ_TRIGGERS      → NEXT_FIRE_TIME, PREV_FIRE_TIME, TRIGGER_STATE updated
 *   2. QRTZ_FIRED_TRIGGERS → row inserted (EXECUTING), then removed after completion
 *   3. QRTZ_SCHEDULER_STATE → LAST_CHECKIN_TIME updated periodically
 *   4. QRTZ_LOCKS          → row-level lock acquired during trigger acquisition
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckJob extends QuartzJobBean {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String jobId = context.getFireInstanceId();
        String triggerKey = context.getTrigger().getKey().toString();
        LocalDateTime fireTime = LocalDateTime.now();

        log.info("[Quartz] HealthCheckJob STARTED | fireId={} | trigger={} | time={}",
                jobId, triggerKey, fireTime.format(FORMATTER));

        try {
            // ── Sample operation: simulate a quick health check ──
            log.info("[Quartz] Running system health check...");

            // TODO: Replace with actual checks
            // Example: Check external service availability, DB connectivity, etc.

            // Simulate some work (500ms)
            Thread.sleep(500);

            log.info("[Quartz] HealthCheckJob COMPLETED | fireId={} | duration=~500ms", jobId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("HealthCheckJob interrupted", e);
        } catch (Exception e) {
            log.error("[Quartz] HealthCheckJob FAILED | fireId={} | error={}", jobId, e.getMessage(), e);
            throw new JobExecutionException("HealthCheckJob failed", e);
        }
    }
}
