package com.hometusk.users.service;

import com.hometusk.households.domain.Household;
import com.hometusk.shared.exception.AccessDeniedException;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public boolean isMember(UUID userId, UUID householdId) {
        return membershipRepository.existsByUser_IdAndHousehold_Id(userId, householdId);
    }

    @Transactional(readOnly = true)
    public void requireMembership(UUID userId, UUID householdId) {
        if (!isMember(userId, householdId)) {
            throw new AccessDeniedException("User is not a member of this household");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Membership> findMembership(UUID userId, UUID householdId) {
        return membershipRepository.findByUser_IdAndHousehold_Id(userId, householdId);
    }

    @Transactional(readOnly = true)
    public List<Membership> findByUserId(UUID userId) {
        return membershipRepository.findByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public List<Membership> findByHouseholdId(UUID householdId) {
        return membershipRepository.findByHousehold_Id(householdId);
    }

    @Transactional(readOnly = true)
    public List<UUID> findHouseholdIdsByUserId(UUID userId) {
        return membershipRepository.findHouseholdIdsByUserId(userId);
    }

    @Transactional
    public Membership addMember(User user, Household household, MembershipRole role) {
        // Check if membership already exists
        Optional<Membership> existing =
                membershipRepository.findByUser_IdAndHousehold_Id(user.getId(), household.getId());

        if (existing.isPresent()) {
            return existing.get();
        }

        Membership membership = new Membership(user, household, role);
        return membershipRepository.save(membership);
    }

    @Transactional
    public void removeMember(UUID userId, UUID householdId) {
        membershipRepository.findByUser_IdAndHousehold_Id(userId, householdId).ifPresent(membershipRepository::delete);
    }
}
