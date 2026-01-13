package com.hometusk.commands.pipeline.guardrails;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that checks if deadline falls in quiet hours.
 *
 * <p><strong>Feature Flag:</strong> This policy is DISABLED by default in Stage 4.
 * Set {@code guardrails.availability-enabled=true} to enable.
 *
 * <p>Rationale: Quiet hours should be household-level configuration, but Stage 4
 * lacks household_settings table. This will be enabled in Stage 5 when
 * household-specific configuration is implemented.
 *
 * <p>Behavior (when enabled):
 * <ul>
 *   <li>If deadline falls in quiet hours (default 22:00-07:00) → CLARIFY</li>
 *   <li>If deadline is NULL or outside quiet hours → ACCEPT</li>
 * </ul>
 *
 * <p>Behavior (when disabled - default):
 * <ul>
 *   <li>Always ACCEPT (skip check)</li>
 * </ul>
 */
@Component
public class AvailabilityPolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityPolicy.class);
    private static final String NAME = "Availability";

    private final GuardrailsConfig config;

    public AvailabilityPolicy(GuardrailsConfig config) {
        this.config = config;
    }

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        // Feature flag check - skip if disabled (default for Stage 4)
        if (!config.isAvailabilityEnabled()) {
            log.debug("AvailabilityPolicy disabled (feature flag OFF), skipping check");
            return GuardrailOutcome.accept();
        }

        for (var action : context.decision().actions()) {
            if (!"create_task".equals(action.actionType())) {
                continue;
            }

            // Check if deadline is specified
            Object deadlineObj = action.parameters().get("deadline");
            if (deadlineObj == null) {
                continue; // No deadline to check
            }

            Instant deadline;
            try {
                deadline = parseInstant(deadlineObj);
            } catch (Exception e) {
                log.warn("AvailabilityPolicy: invalid deadline format: {}", deadlineObj);
                continue; // Let DeadlineSanityPolicy handle format errors
            }

            // Parse quiet hours configuration
            LocalTime quietStart;
            LocalTime quietEnd;
            try {
                quietStart = LocalTime.parse(config.getQuietHoursStart());
                quietEnd = LocalTime.parse(config.getQuietHoursEnd());
            } catch (Exception e) {
                log.error(
                        "AvailabilityPolicy: invalid quiet hours config: start={}, end={}",
                        config.getQuietHoursStart(),
                        config.getQuietHoursEnd(),
                        e);
                return GuardrailOutcome.accept(); // Skip check if config invalid
            }

            // Check if deadline falls in quiet hours
            if (isInQuietHours(deadline, quietStart, quietEnd)) {
                log.warn(
                        "AvailabilityPolicy: deadline in quiet hours: deadline={}, quietHours={}-{}",
                        deadline,
                        quietStart,
                        quietEnd);

                // Calculate next morning suggestion (9:00 AM next day)
                Instant nextMorning = deadline.atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .plusDays(1)
                        .atTime(9, 0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();

                return new GuardrailOutcome.Clarify(
                        "Задача назначена на ночное время. Подтвердите или измените срок.",
                        List.of("deadline"),
                        Map.of("suggested_deadline", nextMorning.toString()));
            }

            log.debug("AvailabilityPolicy: deadline {} outside quiet hours", deadline);
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 250; // Run last, after assignee determined
    }

    /**
     * Check if deadline falls in quiet hours.
     * Handles wrap-around case (e.g., 22:00-07:00 spans midnight).
     */
    private boolean isInQuietHours(Instant deadline, LocalTime quietStart, LocalTime quietEnd) {
        LocalTime deadlineTime = deadline.atZone(ZoneId.systemDefault()).toLocalTime();

        if (quietStart.isBefore(quietEnd)) {
            // Same day range: e.g., 09:00-17:00
            return !deadlineTime.isBefore(quietStart) && deadlineTime.isBefore(quietEnd);
        } else {
            // Wraps midnight: e.g., 22:00-07:00
            return !deadlineTime.isBefore(quietStart) || deadlineTime.isBefore(quietEnd);
        }
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
