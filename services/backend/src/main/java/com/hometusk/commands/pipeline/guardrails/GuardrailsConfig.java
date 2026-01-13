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
}
