package com.hometusk.users.repository;

import com.hometusk.users.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
