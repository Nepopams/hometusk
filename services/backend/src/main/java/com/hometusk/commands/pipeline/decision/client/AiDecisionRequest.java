package com.hometusk.commands.pipeline.decision.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hometusk.commands.pipeline.decision.DecisionContext;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for the upstream AI Platform decision API.
 */
public record AiDecisionRequest(
        @JsonProperty("command_id") String commandId,
        @JsonProperty("user_id") String userId,
        String timestamp,
        String text,
        List<String> capabilities,
        Map<String, Object> context) {

    private static final List<String> DEFAULT_CAPABILITIES =
            List.of("start_job", "propose_create_task", "propose_add_shopping_item", "clarify", "reject");

    public static AiDecisionRequest from(DecisionContext context) {
        return new AiDecisionRequest(
                context.commandId().toString(),
                context.requesterId().toString(),
                Instant.now().toString(),
                extractText(context),
                DEFAULT_CAPABILITIES,
                buildContext(context));
    }

    private static String extractText(DecisionContext context) {
        Map<String, Object> payload = context.payload() != null ? context.payload() : Map.of();

        for (String key : List.of("text", "commandText", "title", "name", "taskId")) {
            Object value = payload.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }

        return context.commandType().name().toLowerCase();
    }

    private static Map<String, Object> buildContext(DecisionContext context) {
        Map<String, Object> householdContext =
                context.householdContext() != null ? context.householdContext() : Map.of();
        Map<String, Object> household = buildHouseholdContext(context, householdContext);

        Map<String, Object> requestContext = new LinkedHashMap<>();
        requestContext.put("household", household);

        Map<String, Object> defaults = buildDefaultsContext(context, householdContext, household);
        if (!defaults.isEmpty()) {
            requestContext.put("defaults", defaults);
        }

        return requestContext;
    }

    private static Map<String, Object> buildHouseholdContext(
            DecisionContext context, Map<String, Object> householdContext) {
        List<Map<String, Object>> members = remapList(
                householdContext.get("members"),
                Map.of("id", "user_id", "name", "display_name", "role", "role", "workload_score", "workload_score"));
        if (members.isEmpty()) {
            members = List.of(Map.of("user_id", context.requesterId().toString()));
        }

        List<Map<String, Object>> zones =
                remapList(householdContext.get("zones"), Map.of("id", "zone_id", "name", "name"));
        List<Map<String, Object>> shoppingLists =
                remapList(householdContext.get("shopping_lists"), Map.of("id", "list_id", "name", "name"));

        return Map.of(
                "household_id", context.householdId().toString(),
                "members", members,
                "zones", zones,
                "shopping_lists", shoppingLists);
    }

    private static Map<String, Object> buildDefaultsContext(
            DecisionContext context, Map<String, Object> householdContext, Map<String, Object> household) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        if (context.requesterId() != null) {
            defaults.put("default_assignee_id", context.requesterId().toString());
        }

        Object defaultListId = householdContext.get("default_list_id");
        if (defaultListId == null) {
            defaultListId = firstShoppingListId(household.get("shopping_lists"));
        }
        if (defaultListId != null && !defaultListId.toString().isBlank()) {
            defaults.put("default_list_id", defaultListId.toString());
        }

        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Object firstShoppingListId(Object rawShoppingLists) {
        if (!(rawShoppingLists instanceof List<?> shoppingLists) || shoppingLists.isEmpty()) {
            return null;
        }
        Object first = shoppingLists.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        return ((Map<String, Object>) firstMap).get("list_id");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> remapList(Object rawList, Map<String, String> fieldMapping) {
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> remapMap((Map<String, Object>) item, fieldMapping))
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static Map<String, Object> remapMap(Map<String, Object> source, Map<String, String> fieldMapping) {
        Map<String, Object> target = new java.util.LinkedHashMap<>();
        fieldMapping.forEach((sourceKey, targetKey) -> {
            Object value = source.get(sourceKey);
            if (value != null) {
                target.put(targetKey, value);
            }
        });
        return target;
    }
}
