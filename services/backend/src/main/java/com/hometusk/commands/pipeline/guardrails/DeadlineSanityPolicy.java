package com.hometusk.commands.pipeline.guardrails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that validates deadline is reasonable (not in past, not too far future).
 *
 * <p>This policy ensures deadlines make sense from a practical perspective.
 * Deadlines that are too far in the future or in the past likely indicate
 * user error or miscommunication.
 *
 * <p>Behavior:
 * <ul>
 *   <li>If deadline is in the past → CLARIFY</li>
 *   <li>If deadline is > maxDeadlineDays (default 365) → CLARIFY</li>
 *   <li>If deadline is NULL or reasonable → ACCEPT</li>
 * </ul>
 */
@Component
public class DeadlineSanityPolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(DeadlineSanityPolicy.class);
    private static final String NAME = "DeadlineSanity";

    private final GuardrailsConfig config;

    public DeadlineSanityPolicy(GuardrailsConfig config) {
        this.config = config;
    }

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        for (var action : context.decision().actions()) {
            if (!"create_task".equals(action.actionType())) {
                continue;
            }

            // Check if deadline is specified
            Object deadlineObj = action.parameters().get("deadline");
            if (deadlineObj == null) {
                continue; // No deadline specified, nothing to validate
            }

            Instant deadline;
            try {
                deadline = parseInstant(deadlineObj);
            } catch (Exception e) {
                log.warn("Invalid deadline format: {}", deadlineObj, e);
                return GuardrailOutcome.clarify(
                        "Не удалось распознать указанный срок. Пожалуйста, укажите дату и время.", List.of("deadline"));
            }

            Instant now = Instant.now();

            // Check if deadline is in the past
            if (deadline.isBefore(now)) {
                log.warn("DeadlineSanityPolicy: deadline in past: deadline={}, now={}", deadline, now);
                return GuardrailOutcome.clarify(
                        "Указанный срок уже прошёл. Пожалуйста, выберите будущую дату.", List.of("deadline"));
            }

            // Check if deadline is too far in the future
            Instant maxDeadline = now.plus(config.getMaxDeadlineDays(), ChronoUnit.DAYS);
            if (deadline.isAfter(maxDeadline)) {
                log.warn(
                        "DeadlineSanityPolicy: deadline too far: deadline={}, maxDays={}",
                        deadline,
                        config.getMaxDeadlineDays());
                return GuardrailOutcome.clarify(
                        String.format(
                                "Указанный срок слишком далеко (%d+ дней). Подтвердите или измените дату.",
                                config.getMaxDeadlineDays()),
                        List.of("deadline"));
            }

            log.debug("DeadlineSanityPolicy: deadline {} is reasonable", deadline);
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 150; // After zone owner assignment, before workload checks
    }

    /**
     * Parse deadline from various formats (Instant, String).
     */
    private Instant parseInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }
}
