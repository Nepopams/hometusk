package com.hometusk.commands.service;

import com.hometusk.commands.domain.CommandStatus;
import com.hometusk.commands.repository.CommandRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CommandSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CommandSchedulerService.class);

    private final CommandRepository commandRepository;
    private final CommandService commandService;
    private final int batchSize;

    public CommandSchedulerService(
            CommandRepository commandRepository,
            CommandService commandService,
            @Value("${hometusk.command-scheduler.batch-size:50}") int batchSize) {
        this.commandRepository = commandRepository;
        this.commandService = commandService;
        this.batchSize = Math.max(1, batchSize);
    }

    public SchedulerResult runDueCommands() {
        var dueIds = commandRepository.findDueScheduledCommandIds(
                CommandStatus.SCHEDULED, Instant.now(), PageRequest.of(0, batchSize));

        int processed = 0;
        int skipped = 0;
        int errors = 0;

        for (UUID commandId : dueIds) {
            try {
                if (commandService.executeScheduledCommand(commandId, UUID.randomUUID())) {
                    processed++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException e) {
                errors++;
                log.error("Scheduled command failed unexpectedly: commandId={}", commandId, e);
            }
        }

        return new SchedulerResult(dueIds.size(), processed, skipped, errors);
    }

    public record SchedulerResult(int candidates, int processed, int skipped, int errors) {}
}
