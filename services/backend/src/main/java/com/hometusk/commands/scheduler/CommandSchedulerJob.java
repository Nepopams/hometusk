package com.hometusk.commands.scheduler;

import com.hometusk.commands.service.CommandSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hometusk.command-scheduler.enabled", havingValue = "true")
public class CommandSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(CommandSchedulerJob.class);

    private final CommandSchedulerService commandSchedulerService;

    public CommandSchedulerJob(CommandSchedulerService commandSchedulerService) {
        this.commandSchedulerService = commandSchedulerService;
    }

    @Scheduled(fixedRateString = "${hometusk.command-scheduler.fixed-rate-ms:60000}")
    public void runDueCommands() {
        var result = commandSchedulerService.runDueCommands();
        if (result.candidates() > 0) {
            log.info(
                    "Scheduled command run finished: candidates={}, processed={}, skipped={}, errors={}",
                    result.candidates(),
                    result.processed(),
                    result.skipped(),
                    result.errors());
        }
    }
}
