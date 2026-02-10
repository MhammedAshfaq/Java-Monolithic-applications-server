package com.javainfraexample.spring_monolith_template.scheduler.jobs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

// import com.javainfraexample.spring_monolith_template.common.job.JobProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Job: Runs every day at midnight (00:00:00).
 * 
 * DB-backed → survives restarts, supports clustering, handles misfires.
 * 
 * PRODUCTION RULE: Keep Quartz jobs THIN.
 * - Don't put heavy business logic here.
 * - Publish events or enqueue RabbitMQ jobs for heavy processing.
 * 
 * Use cases:
 *   - Clean up expired tokens/sessions
 *   - Archive old records
 *   - Generate daily reports (dispatch to RabbitMQ)
 *   - Send daily digest emails (dispatch to RabbitMQ)
 *   - Reset daily counters in Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCleanupJob extends QuartzJobBean {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Uncomment when ready to dispatch heavy work to RabbitMQ:
    // private final JobProducer jobProducer;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[Quartz] Daily cleanup job started at: {}", LocalDateTime.now().format(FORMATTER));

        try {
            // ── Sample operation: log execution ──
            log.info("[Quartz] Running daily cleanup tasks...");

            // TODO: Replace with actual daily tasks
            // Example 1: Clean expired tokens
            // refreshTokenRepository.deleteExpiredTokens();

            // Example 2: Dispatch report generation to RabbitMQ (keeps Quartz thin)
            // jobProducer.sendJob(JobType.REPORT, "daily-report", Map.of(
            //     "date", LocalDate.now().minusDays(1).toString()
            // ));

            // Example 3: Dispatch daily digest emails to RabbitMQ
            // jobProducer.sendEmail("daily-digest", Map.of(
            //     "date", LocalDate.now().toString()
            // ));

            log.info("[Quartz] Daily cleanup job completed successfully");

        } catch (Exception e) {
            log.error("[Quartz] Daily cleanup job failed: {}", e.getMessage(), e);
            throw new JobExecutionException("Daily cleanup failed", e);
        }
    }
}
