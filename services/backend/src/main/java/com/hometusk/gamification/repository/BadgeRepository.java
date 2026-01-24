package com.hometusk.gamification.repository;

import com.hometusk.gamification.domain.Badge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    Optional<Badge> findByCode(String code);

    List<Badge> findAllByOrderByNameAsc();
}
