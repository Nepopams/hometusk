package com.hometusk.users.domain;

public enum EmailSource {
    IDP_CLAIM("idp_claim"),
    MANUAL("manual"),
    UNKNOWN("unknown");

    private final String value;

    EmailSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EmailSource fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        for (EmailSource source : values()) {
            if (source.value.equals(value)) {
                return source;
            }
        }

        throw new IllegalArgumentException("Unknown email source: " + value);
    }
}
