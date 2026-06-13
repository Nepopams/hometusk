package com.hometusk.users.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.users.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailNotificationEligibilityPolicy")
class EmailNotificationEligibilityPolicyTest {

    private final EmailNotificationEligibilityPolicy policy = new EmailNotificationEligibilityPolicy();

    @Test
    @DisplayName("Should allow notifications only when email is present and verified")
    void allowsOnlyPresentVerifiedEmail() {
        User user = new User("ext-1", "user@example.com", "User");
        user.setEmailVerified(true);

        assertThat(policy.isEligible(user)).isTrue();
    }

    @Test
    @DisplayName("Should reject users without verified email")
    void rejectsUnverifiedEmail() {
        User user = new User("ext-1", "user@example.com", "User");

        assertThat(policy.isEligible(user)).isFalse();
    }

    @Test
    @DisplayName("Should reject missing email")
    void rejectsMissingEmail() {
        User user = new User("ext-1", null, "User");
        user.setEmailVerified(true);

        assertThat(policy.isEligible(user)).isFalse();
    }
}
