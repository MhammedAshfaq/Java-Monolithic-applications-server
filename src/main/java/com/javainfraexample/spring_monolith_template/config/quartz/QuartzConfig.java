package com.javainfraexample.spring_monolith_template.config.quartz;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.javainfraexample.spring_monolith_template.scheduler.jobs.DailyCleanupJob;
import com.javainfraexample.spring_monolith_template.scheduler.jobs.HealthCheckJob;
import com.javainfraexample.spring_monolith_template.scheduler.jobs.WeeklyReportJob;

/**
 * Quartz Job + Trigger registration.
 * 
 * Architecture:
 *   ┌─────────────┐
 *   │ Quartz      │  decides WHEN (cron, DB-backed, clustered)
 *   │ (this file) │
 *   └──────┬──────┘
 *          │ fires job
 *   ┌──────▼──────┐
 *   │ Quartz Job  │  keeps it THIN → dispatches to RabbitMQ
 *   │ (scheduler/ │
 *   │   jobs/)    │
 *   └──────┬──────┘
 *          │ (future: enqueue via JobProducer)
 *   ┌──────▼──────┐
 *   │ RabbitMQ    │  handles HOW (workers, retry, DLQ)
 *   │ (common/    │
 *   │   job/)     │
 *   └─────────────┘
 * 
 * Misfire handling:
 *   - withMisfireHandlingInstructionFireAndProceed()
 *     → If the server was down at trigger time, run immediately when it comes back.
 */
@Configuration
public class QuartzConfig {

    // ==================== Daily Cleanup Job (Every midnight) ====================

    @Bean
    public JobDetail dailyCleanupJobDetail() {
        return JobBuilder.newJob(DailyCleanupJob.class)
                .withIdentity("dailyCleanupJob", "scheduled-jobs")
                .withDescription("Runs daily at midnight - cleanup, reports, digest")
                .storeDurably()
                .requestRecovery(true) // re-execute if app crashes mid-execution
                .build();
    }

    @Bean
    public Trigger dailyCleanupTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(dailyCleanupJobDetail())
                .withIdentity("dailyCleanupTrigger", "scheduled-triggers")
                .withDescription("Fires every day at 00:00:00")
                .withSchedule(
                    CronScheduleBuilder
                        .dailyAtHourAndMinute(0, 0) // midnight
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();
    }

    // ==================== Health Check Job (Every 3 minutes) ====================

    @Bean
    public JobDetail healthCheckJobDetail() {
        return JobBuilder.newJob(HealthCheckJob.class)
                .withIdentity("healthCheckJob", "scheduled-jobs")
                .withDescription("Runs every 3 minutes - system health check (observable)")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger healthCheckTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(healthCheckJobDetail())
                .withIdentity("healthCheckTrigger", "scheduled-triggers")
                .withDescription("Fires every 3 minutes")
                .withSchedule(
                    CronScheduleBuilder
                        .cronSchedule("0 */3 * * * ?") // every 3 minutes
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();
    }

    // ==================== Weekly Report Job (Every Monday at midnight) ====================

    @Bean
    public JobDetail weeklyReportJobDetail() {
        return JobBuilder.newJob(WeeklyReportJob.class)
                .withIdentity("weeklyReportJob", "scheduled-jobs")
                .withDescription("Runs every Monday at midnight - weekly reports, cleanup")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger weeklyReportTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(weeklyReportJobDetail())
                .withIdentity("weeklyReportTrigger", "scheduled-triggers")
                .withDescription("Fires every Monday at 00:00:00")
                .withSchedule(
                    CronScheduleBuilder
                        .cronSchedule("0 0 0 ? * MON") // every Monday at midnight
                        .withMisfireHandlingInstructionFireAndProceed()
                )
                .build();
    }
}
