package com.hometusk.users.service;

import com.hometusk.users.domain.User;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationEligibilityPolicy {

    public boolean isEligible(User user) {
        return user != null && user.isEmailNotificationEligible();
    }
}
