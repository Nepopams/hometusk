package com.hometusk.users.service;

import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.shared.security.JwtClaimsExtractor;
import com.hometusk.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserResolver {

    private static final Logger log = LoggerFactory.getLogger(UserResolver.class);

    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final UserService userService;

    public UserResolver(JwtClaimsExtractor jwtClaimsExtractor, UserService userService) {
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.userService = userService;
    }

    @Transactional
    public CurrentUser resolveCurrentUser() {
        JwtClaimsExtractor.JwtClaims claims = jwtClaimsExtractor.extractClaims();

        if (claims == null || claims.sub() == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        User user = userService
                .findByExternalId(claims.sub())
                .map(existingUser -> updateUserIfNeeded(existingUser, claims))
                .orElseGet(() -> createUser(claims));

        // Set user ID in MDC for logging
        MDC.put(MdcKeys.USER_ID, user.getId().toString());

        return CurrentUser.of(user.getId(), user.getExternalId(), user.getEmail(), user.getDisplayName());
    }

    private User updateUserIfNeeded(User user, JwtClaimsExtractor.JwtClaims claims) {
        boolean updated = false;

        if (user.syncEmailFromIdentityProvider(claims.email(), claims.emailVerified())) {
            updated = true;
        }

        if (claims.displayName() != null && !claims.displayName().equals(user.getDisplayName())) {
            user.setDisplayName(claims.displayName());
            updated = true;
        }

        if (updated) {
            log.info("Updated user profile: {}", user.getId());
            return userService.update(user);
        }

        return user;
    }

    private User createUser(JwtClaimsExtractor.JwtClaims claims) {
        log.info("Creating new user for external ID: {}", claims.sub());
        return userService.createFromIdentityClaims(
                claims.sub(), claims.email(), claims.emailVerified(), claims.displayName());
    }
}
