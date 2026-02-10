package com.javainfraexample.spring_monolith_template.config.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduler configuration - enables @Scheduled annotations with a configurable thread pool.
 * 
 * Spring Boot Scheduler runs inside the application process (no external dependency needed).
 * For distributed/heavy jobs, use RabbitMQ background jobs instead.
 * 
 * By default, Spring uses a single-threaded scheduler. This config creates a thread pool
 * so multiple cron jobs can run concurrently without blocking each other.
 * 
 * Cron Expression Format: second minute hour day-of-month month day-of-week
 *   - "0 0 0 * * *"        = Every midnight
 *   - "0 0 0 * * MON"      = Every Monday at midnight
 *   - "0/5 * * * * *"      = Every 5 seconds (polling)
 *   - "0 0 * * * *"        = Every hour
 *   - "0 30 9 * * MON-FRI" = Weekdays at 9:30 AM
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(2); // number of concurrent cron jobs
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();

        taskRegistrar.setTaskScheduler(scheduler);
    }
}
