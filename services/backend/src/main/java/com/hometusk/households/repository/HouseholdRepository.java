package com.hometusk.households.repository;

import com.hometusk.households.domain.Household;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {}
