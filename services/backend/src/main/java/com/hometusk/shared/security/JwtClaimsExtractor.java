package com.hometusk.shared.security;

import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtClaimsExtractor {

    public JwtClaims extractClaims() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        String sub = jwt.getClaimAsString("sub");
        String email = normalizeEmail(jwt.getClaimAsString("email"));
        Boolean emailVerified = extractEmailVerified(jwt);
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");

        // Build display name from available claims
        String displayName = buildDisplayName(name, givenName, familyName, email, sub);

        return new JwtClaims(sub, email, emailVerified, displayName);
    }

    private String buildDisplayName(String name, String givenName, String familyName, String email, String sub) {
        if (name != null && !name.isBlank()) {
            return name;
        }

        if (givenName != null && !givenName.isBlank()) {
            if (familyName != null && !familyName.isBlank()) {
                return givenName + " " + familyName;
            }
            return givenName;
        }

        if (email != null && !email.isBlank()) {
            return email.split("@")[0];
        }

        return sub != null ? sub.substring(0, Math.min(8, sub.length())) : "Unknown";
    }

    private Boolean extractEmailVerified(Jwt jwt) {
        Object claim = jwt.getClaims().get("email_verified");
        if (claim instanceof Boolean value) {
            return value;
        }
        if (claim instanceof String value) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
        }
        return null;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    public record JwtClaims(String sub, String email, Boolean emailVerified, String displayName) {}
}
