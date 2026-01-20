package com.hometusk.commands.pipeline.guardrails;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for guardrails policies.
 */
@Configuration
@ConfigurationProperties(prefix = "guardrails")
public class GuardrailsConfig {

    private boolean enabled = true;
    private int maxOpenTasksPerAssignee = 10;
    private int maxDeadlineDays = 365;

    // Availability feature (OFF by default for Stage 4)
    private boolean availabilityEnabled = false;
    private String quietHoursStart = "22:00";
    private String quietHoursEnd = "07:00";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxOpenTasksPerAssignee() {
        return maxOpenTasksPerAssignee;
    }

    public void setMaxOpenTasksPerAssignee(int maxOpenTasksPerAssignee) {
        this.maxOpenTasksPerAssignee = maxOpenTasksPerAssignee;
    }

    public int getMaxDeadlineDays() {
        return maxDeadlineDays;
    }

    public void setMaxDeadlineDays(int maxDeadlineDays) {
        this.maxDeadlineDays = maxDeadlineDays;
    }

    public boolean isAvailabilityEnabled() {
        return availabilityEnabled;
    }

    public void setAvailabilityEnabled(boolean availabilityEnabled) {
        this.availabilityEnabled = availabilityEnabled;
    }

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
