package com.hometusk.shared.security;

import java.util.UUID;

public record CurrentUser(UUID id, String externalId, String email, String displayName) {

    public static CurrentUser of(UUID id, String externalId, String email, String displayName) {
        return new CurrentUser(id, externalId, email, displayName);
    }
}
