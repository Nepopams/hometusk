package com.hometusk.users.service;

import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId);
    }

    @Transactional
    public User create(String externalId, String email, String displayName) {
        User user = new User(externalId, email, displayName);
        return userRepository.save(user);
    }

    @Transactional
    public User update(User user) {
        return userRepository.save(user);
    }
}
