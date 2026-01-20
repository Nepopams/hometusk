package com.hometusk.commands.repository;

import com.hometusk.commands.domain.DecisionLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionLogRepository extends JpaRepository<DecisionLog, UUID> {

    Optional<DecisionLog> findByCommandId(UUID commandId);

    Optional<DecisionLog> findByCorrelationId(UUID correlationId);

    List<DecisionLog> findBySchemaValidFalseOrderByCreatedAtDesc();

    List<DecisionLog> findByBusinessValidFalseOrderByCreatedAtDesc();
}
