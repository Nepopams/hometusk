package com.hometusk.asr.repository;

import com.hometusk.asr.domain.AsrTranscriptionRef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsrTranscriptionRefRepository extends JpaRepository<AsrTranscriptionRef, UUID> {

    Optional<AsrTranscriptionRef> findByTranscriptionId(UUID transcriptionId);
}
