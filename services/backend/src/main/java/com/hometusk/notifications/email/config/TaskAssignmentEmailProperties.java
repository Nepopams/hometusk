package com.hometusk.notifications.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hometusk.email.task-assignment")
public class TaskAssignmentEmailProperties {

    private boolean enabled = true;
    private boolean skipSelfNotifications = true;
    private String appBaseUrl = "http://localhost:5173";
    private String taskPathTemplate = "/households/{householdId}/tasks/{taskId}";
    private String subjectTemplate = "HomeTusk: task assigned";
    private String bodyTextTemplate =
            """
            Hello {{assigneeName}},

            {{actorName}} assigned you a task in {{householdName}}.

            Task: {{taskTitle}}
            Zone: {{zoneName}}
            Due: {{deadline}}

            Open: {{taskUrl}}
            """;
    private String bodyHtmlTemplate =
            """
            <p>Hello {{assigneeName}},</p>
            <p>{{actorName}} assigned you a task in {{householdName}}.</p>
            <p><strong>Task:</strong> {{taskTitle}}<br>
            <strong>Zone:</strong> {{zoneName}}<br>
            <strong>Due:</strong> {{deadline}}</p>
            <p><a href="{{taskUrl}}">Open task</a></p>
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSkipSelfNotifications() {
        return skipSelfNotifications;
    }

    public void setSkipSelfNotifications(boolean skipSelfNotifications) {
        this.skipSelfNotifications = skipSelfNotifications;
    }

    public String getAppBaseUrl() {
        return appBaseUrl;
    }

    public void setAppBaseUrl(String appBaseUrl) {
        this.appBaseUrl = appBaseUrl == null || appBaseUrl.isBlank() ? "" : trimTrailingSlash(appBaseUrl);
    }

    public String getTaskPathTemplate() {
        return taskPathTemplate;
    }

    public void setTaskPathTemplate(String taskPathTemplate) {
        this.taskPathTemplate = taskPathTemplate == null || taskPathTemplate.isBlank()
                ? "/households/{householdId}/tasks/{taskId}"
                : taskPathTemplate.trim();
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate == null || subjectTemplate.isBlank()
                ? "HomeTusk: task assigned"
                : subjectTemplate.strip();
    }

    public String getBodyTextTemplate() {
        return bodyTextTemplate;
    }

    public void setBodyTextTemplate(String bodyTextTemplate) {
        this.bodyTextTemplate = bodyTextTemplate;
    }

    public String getBodyHtmlTemplate() {
        return bodyHtmlTemplate;
    }

    public void setBodyHtmlTemplate(String bodyHtmlTemplate) {
        this.bodyHtmlTemplate = bodyHtmlTemplate;
    }

    public String buildTaskUrl(String householdId, String taskId) {
        String path = taskPathTemplate.replace("{householdId}", householdId).replace("{taskId}", taskId);
        if (appBaseUrl == null || appBaseUrl.isBlank()) {
            return path;
        }
        return trimTrailingSlash(appBaseUrl) + (path.startsWith("/") ? path : "/" + path);
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
