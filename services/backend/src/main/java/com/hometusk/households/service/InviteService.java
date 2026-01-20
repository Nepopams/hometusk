package com.hometusk.households.service;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.HouseholdInvite;
import com.hometusk.households.domain.InviteStatus;
import com.hometusk.households.dto.AcceptInviteResponse;
import com.hometusk.households.dto.CreateInviteResponse;
import com.hometusk.households.repository.HouseholdInviteRepository;
import com.hometusk.notifications.service.NotificationService;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {

    private static final Logger log = LoggerFactory.getLogger(InviteService.class);
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final HouseholdInviteRepository inviteRepository;
    private final InviteTokenGenerator tokenGenerator;
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final HouseholdService householdService;
    private final UserService userService;
    private final NotificationService notificationService;

    public InviteService(
            HouseholdInviteRepository inviteRepository,
            InviteTokenGenerator tokenGenerator,
            MembershipService membershipService,
            MembershipRepository membershipRepository,
            HouseholdService householdService,
            UserService userService,
            NotificationService notificationService) {
        this.inviteRepository = inviteRepository;
        this.tokenGenerator = tokenGenerator;
        this.membershipService = membershipService;
        this.membershipRepository = membershipRepository;
        this.householdService = householdService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CreateInviteResponse createInvite(UUID householdId, UUID initiatorUserId) {
        membershipService.requireMembership(initiatorUserId, householdId);

        Household household = householdService.getById(householdId);
        User initiator = userService.getById(initiatorUserId);

        String token = tokenGenerator.generateToken();
        Instant expiresAt = Instant.now().plus(INVITE_TTL);

        HouseholdInvite invite = new HouseholdInvite(household, initiator, token, expiresAt);
        inviteRepository.save(invite);

        log.info("Created household invite: householdId={}, inviteId={}", householdId, invite.getId());
        return CreateInviteResponse.from(invite, null);
    }

    @Transactional
    public AcceptInviteResponse acceptInvite(String inviteToken, UUID userId, UUID correlationId) {
        HouseholdInvite invite = inviteRepository
                .findByInviteTokenForUpdate(inviteToken)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_TOKEN, "Invite token not found"));

        Instant now = Instant.now();

        if (invite.getStatus() != InviteStatus.ACTIVE) {
            throw inviteUnavailable(invite.getStatus());
        }

        if (invite.isExpired(now)) {
            invite.markExpired();
            inviteRepository.save(invite);
            throw new BusinessException(ErrorCode.INVITE_EXPIRED, "Invite token expired");
        }

        Optional<Membership> existing = membershipRepository.findByUserIdAndHouseholdId(
                userId, invite.getHousehold().getId());
        if (existing.isPresent()) {
            log.info("Invite accept no-op: user already member, invite remains active");
            return AcceptInviteResponse.from(existing.get());
        }

        User user = userService.getById(userId);
        Membership membership = membershipService.addMember(user, invite.getHousehold(), MembershipRole.member);

        invite.markRedeemed(user, now);
        inviteRepository.save(invite);

        notificationService.notifyInviteAccepted(invite, user, correlationId);

        log.info("Invite redeemed: inviteId={}, userId={}", invite.getId(), userId);
        return AcceptInviteResponse.from(membership);
    }

    private BusinessException inviteUnavailable(InviteStatus status) {
        return switch (status) {
            case EXPIRED -> new BusinessException(ErrorCode.INVITE_EXPIRED, "Invite token expired");
            case REDEEMED -> new BusinessException(ErrorCode.INVITE_REDEEMED, "Invite token already redeemed");
            case REVOKED -> new BusinessException(ErrorCode.INVITE_REVOKED, "Invite token revoked");
            default -> new BusinessException(ErrorCode.INVITE_REDEEMED, "Invite token unavailable");
        };
    }
}
