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
 * Quartz Job: Runs every Monday at midnight (00:00:00).
 * 
 * DB-backed → survives restarts, supports clustering, handles misfires.
 * 
 * PRODUCTION RULE: Keep Quartz jobs THIN.
 * - Don't put heavy business logic here.
 * - Publish events or enqueue RabbitMQ jobs for heavy processing.
 * 
 * Use cases:
 *   - Generate weekly analytics report (dispatch to RabbitMQ)
 *   - Send weekly summary emails (dispatch to RabbitMQ)
 *   - Clean up old data (>30 days)
 *   - Database maintenance tasks
 *   - Reset weekly rate limit counters
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportJob extends QuartzJobBean {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Uncomment when ready to dispatch heavy work to RabbitMQ:
    // private final JobProducer jobProducer;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("[Quartz] Weekly report job started at: {}", LocalDateTime.now().format(FORMATTER));

        try {
            // ── Sample operation: log execution ──
            log.info("[Quartz] Running weekly tasks...");

            // TODO: Replace with actual weekly tasks
            // Example 1: Dispatch weekly report to RabbitMQ (keeps Quartz thin)
            // jobProducer.sendJob(JobType.REPORT, "weekly-report", Map.of(
            //     "weekStart", LocalDate.now().minusWeeks(1).toString(),
            //     "weekEnd", LocalDate.now().toString()
            // ));

            // Example 2: Send weekly summary email
            // jobProducer.sendEmail("weekly-summary", Map.of(
            //     "period", "last-7-days"
            // ));

            // Example 3: Clean up old data
            // auditLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(90));

            log.info("[Quartz] Weekly report job completed successfully");

        } catch (Exception e) {
            log.error("[Quartz] Weekly report job failed: {}", e.getMessage(), e);
            throw new JobExecutionException("Weekly report failed", e);
        }
    }
}
