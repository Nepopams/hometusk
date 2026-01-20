package com.hometusk.shared.security;

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
        String email = jwt.getClaimAsString("email");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");

        // Build display name from available claims
        String displayName = buildDisplayName(name, givenName, familyName, email, sub);

        return new JwtClaims(sub, email, displayName);
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

    public record JwtClaims(String sub, String email, String displayName) {}
}
