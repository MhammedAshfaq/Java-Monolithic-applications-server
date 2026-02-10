package com.javainfraexample.spring_monolith_template.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight scheduled tasks using Spring @Scheduled (in-memory, no DB).
 * 
 * Use @Scheduled for:
 *   - Simple polling / heartbeat tasks
 *   - Non-critical recurring checks
 *   - Tasks that are OK to miss on restart
 * 
 * Use Quartz (scheduler/jobs/) for:
 *   - Business-critical cron jobs (midnight, weekly, etc.)
 *   - Jobs that must not be missed on restart
 *   - Clustered / multi-instance environments
 * 
 * @see com.javainfraexample.spring_monolith_template.config.quartz.QuartzConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleScheduledTasks {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Polling Task (Every 5 minutes) ====================

    /**
     * Runs every 5 minutes (lightweight, in-memory).
     * Use case: Health check polling, status sync, queue monitoring, etc.
     */
    @Scheduled(fixedRate = 300000) // 5 * 60 * 1000 = 300,000ms
    public void pollingTask() {
        log.debug("Polling task executed at: {}", LocalDateTime.now().format(FORMATTER));

        // TODO: Replace with actual polling logic
        // Examples:
        //   - Check external API status
        //   - Sync data from external system
        //   - Monitor queue depth
        //   - Clean up expired sessions
    }
}
