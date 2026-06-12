package com.hometusk.users.service;

import static org.mockito.Mockito.verify;

import com.hometusk.users.repository.MembershipRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipService")
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Test
    @DisplayName("findByUserId fetches households for profile DTOs")
    void findByUserIdFetchesHouseholds() {
        MembershipService service = new MembershipService(membershipRepository);
        UUID userId = UUID.randomUUID();

        service.findByUserId(userId);

        verify(membershipRepository).findByUser_IdWithHousehold(userId);
    }

    @Test
    @DisplayName("findByHouseholdId fetches users for member DTOs")
    void findByHouseholdIdFetchesUsers() {
        MembershipService service = new MembershipService(membershipRepository);
        UUID householdId = UUID.randomUUID();

        service.findByHouseholdId(householdId);

        verify(membershipRepository).findByHousehold_IdWithUser(householdId);
    }
}
