package com.hometusk.commands.pipeline.guardrails;

import com.hometusk.shopping.domain.ShoppingItemCategory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Policy that validates shopping item parameters (Stage 5).
 *
 * <p>Validates:
 * <ul>
 *   <li>Item name is non-empty</li>
 *   <li>Item name is not too long (max 255 chars)</li>
 *   <li>Category, when present, is from the supported taxonomy</li>
 * </ul>
 *
 * <p>Behavior:
 * <ul>
 *   <li>If name is empty/blank → REJECT</li>
 *   <li>If name is too long → CLARIFY</li>
 *   <li>Otherwise → ACCEPT</li>
 * </ul>
 */
@Component
public class ShoppingItemValidationPolicy implements GuardrailPolicy {

    private static final Logger log = LoggerFactory.getLogger(ShoppingItemValidationPolicy.class);
    private static final String NAME = "ShoppingItemValidation";
    private static final int MAX_ITEM_NAME_LENGTH = 255;
    private static final int MAX_SOURCE_LENGTH = 120;

    @Override
    public GuardrailOutcome evaluate(GuardrailContext context) {
        for (var action : context.decision().actions()) {
            if (!"add_shopping_item".equals(action.actionType())) {
                continue;
            }

            Object nameObj = action.parameters().get("name");
            String name = nameObj != null ? nameObj.toString() : null;

            // Validate non-empty
            if (name == null || name.isBlank()) {
                log.warn("ShoppingItemValidationPolicy: empty item name, correlationId={}", context.correlationId());
                return GuardrailOutcome.reject("Название товара не может быть пустым.", "SHOPPING_ITEM_NAME_EMPTY");
            }

            // Validate length
            if (name.length() > MAX_ITEM_NAME_LENGTH) {
                log.warn(
                        "ShoppingItemValidationPolicy: name too long ({}), correlationId={}",
                        name.length(),
                        context.correlationId());
                return GuardrailOutcome.clarify(
                        String.format(
                                "Название товара слишком длинное (максимум %d символов). Пожалуйста, сократите.",
                                MAX_ITEM_NAME_LENGTH),
                        List.of("name"));
            }

            Object categoryObj = action.parameters().get("category");
            if (categoryObj != null && !ShoppingItemCategory.isAllowed(categoryObj.toString())) {
                log.warn("ShoppingItemValidationPolicy: invalid category, correlationId={}", context.correlationId());
                return GuardrailOutcome.reject(
                        "Категория покупки не поддерживается.",
                        "SHOPPING_ITEM_CATEGORY_INVALID");
            }

            Object sourceObj = action.parameters().get("source");
            String source = sourceObj != null ? sourceObj.toString() : null;
            if (source != null && source.trim().length() > MAX_SOURCE_LENGTH) {
                log.warn(
                        "ShoppingItemValidationPolicy: source too long ({}), correlationId={}",
                        source.trim().length(),
                        context.correlationId());
                return GuardrailOutcome.clarify(
                        String.format(
                                "Источник покупки слишком длинный (максимум %d символов). Пожалуйста, сократите.",
                                MAX_SOURCE_LENGTH),
                        List.of("source"));
            }

            log.debug(
                    "ShoppingItemValidationPolicy: item name '{}' is valid, correlationId={}",
                    name,
                    context.correlationId());
        }

        return GuardrailOutcome.accept();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return 75; // Early validation, before other policies
    }
}
