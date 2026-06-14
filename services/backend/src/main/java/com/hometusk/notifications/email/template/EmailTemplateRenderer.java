package com.hometusk.notifications.email.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    public RenderedEmailContent render(
            String subjectTemplate, String bodyTextTemplate, String bodyHtmlTemplate, Map<String, String> variables) {
        Map<String, String> safeVariables = variables == null ? Map.of() : variables;
        return new RenderedEmailContent(
                renderNullable(subjectTemplate, safeVariables),
                renderNullable(bodyTextTemplate, safeVariables),
                renderNullable(bodyHtmlTemplate, safeVariables));
    }

    private static String renderNullable(String template, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String replacement = variables.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    public record RenderedEmailContent(String subject, String bodyText, String bodyHtml) {}
}
