package com.hometusk.notifications.email.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailTemplateRendererTest {

    private final EmailTemplateRenderer renderer = new EmailTemplateRenderer();

    @Test
    void render_replacesTokensInSubjectTextAndHtml() {
        var result = renderer.render(
                "Task {{ taskTitle }}",
                "Assigned to {{assignee}}",
                "<strong>{{ taskTitle }}</strong>",
                Map.of("taskTitle", "Clean kitchen", "assignee", "Alice"));

        assertThat(result.subject()).isEqualTo("Task Clean kitchen");
        assertThat(result.bodyText()).isEqualTo("Assigned to Alice");
        assertThat(result.bodyHtml()).isEqualTo("<strong>Clean kitchen</strong>");
    }

    @Test
    void render_replacesMissingTokensWithEmptyString() {
        var result = renderer.render("Task {{missing}}", "Body", null, Map.of());

        assertThat(result.subject()).isEqualTo("Task ");
        assertThat(result.bodyText()).isEqualTo("Body");
        assertThat(result.bodyHtml()).isNull();
    }
}
