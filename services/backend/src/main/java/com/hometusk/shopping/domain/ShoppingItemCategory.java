package com.hometusk.shopping.domain;

import java.util.Locale;
import java.util.Set;

public enum ShoppingItemCategory {
    GROCERIES("groceries"),
    CLEANING("cleaning"),
    PERSONAL_CARE("personal_care"),
    DIY("diy"),
    ELECTRONICS("electronics"),
    OTHER("other");

    private static final Set<String> VALUES = Set.of(
            GROCERIES.value, CLEANING.value, PERSONAL_CARE.value, DIY.value, ELECTRONICS.value, OTHER.value);

    private final String value;

    ShoppingItemCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static String normalize(String category) {
        if (category == null) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowed(String category) {
        String normalized = normalize(category);
        return normalized != null && VALUES.contains(normalized);
    }
}
