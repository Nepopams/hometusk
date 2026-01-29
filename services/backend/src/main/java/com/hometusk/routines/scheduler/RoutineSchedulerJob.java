package com.hometusk.routines.scheduler;

import com.hometusk.routines.service.RoutineSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hometusk.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class RoutineSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(RoutineSchedulerJob.class);

    private final RoutineSchedulerService schedulerService;

    public RoutineSchedulerJob(RoutineSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedRateString = "${hometusk.scheduler.fixed-rate-ms:3600000}")
    public void runScheduler() {
        log.info("Scheduled routine task generation triggered");
        try {
            var result = schedulerService.generateUpcomingTasks();
            log.info("Scheduled run completed: {}", result);
        } catch (Exception e) {
            log.error("Scheduled run failed", e);
        }
    }
}
