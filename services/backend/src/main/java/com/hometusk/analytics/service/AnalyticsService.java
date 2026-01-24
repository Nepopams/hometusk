package com.hometusk.analytics.service;

import com.hometusk.analytics.dto.AnalyticsSummaryResponse;
import com.hometusk.analytics.dto.FairnessInfo;
import com.hometusk.analytics.dto.MemberStats;
import com.hometusk.analytics.dto.OverdueTask;
import com.hometusk.analytics.dto.ZoneStats;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.repository.MembershipRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final String UNASSIGNED_NAME = "Unassigned";
    private static final UUID UNASSIGNED_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String FORMULA = "Balance = 100 × (1 - Gini coefficient)";

    private final TaskRepository taskRepository;
    private final MembershipRepository membershipRepository;
    private final ZoneRepository zoneRepository;

    public AnalyticsService(
            TaskRepository taskRepository, MembershipRepository membershipRepository, ZoneRepository zoneRepository) {
        this.taskRepository = taskRepository;
        this.membershipRepository = membershipRepository;
        this.zoneRepository = zoneRepository;
    }

    public AnalyticsSummaryResponse getAnalytics(UUID householdId, String period) {
        Instant now = Instant.now();
        String resolvedPeriod = "30d".equals(period) ? "30d" : "7d";
        Instant periodStart = calculatePeriodStart(resolvedPeriod, now);
        Instant periodEnd = now;

        List<Membership> memberships = membershipRepository.findByHousehold_IdWithUser(householdId);

        Counts completedCounts =
                toCounts(taskRepository.countCompletedByAssigneeInPeriod(householdId, periodStart, periodEnd));
        Counts overdueCounts = toCounts(taskRepository.countOverdueByAssignee(householdId, now));
        Counts openCounts = toCounts(taskRepository.countOpenByAssignee(householdId, now));

        List<MemberStats> perMember = memberships.stream()
                .map(membership -> {
                    UUID memberId = membership.getUser().getId();
                    String memberName = safeDisplayName(membership.getUser().getDisplayName());
                    return new MemberStats(
                            memberId,
                            memberName,
                            toInt(completedCounts.byId().getOrDefault(memberId, 0L)),
                            toInt(overdueCounts.byId().getOrDefault(memberId, 0L)),
                            toInt(openCounts.byId().getOrDefault(memberId, 0L)));
                })
                .collect(Collectors.toCollection(ArrayList::new));

        if (completedCounts.unassigned() > 0 || overdueCounts.unassigned() > 0 || openCounts.unassigned() > 0) {
            perMember.add(new MemberStats(
                    UNASSIGNED_ID,
                    UNASSIGNED_NAME,
                    toInt(completedCounts.unassigned()),
                    toInt(overdueCounts.unassigned()),
                    toInt(openCounts.unassigned())));
        }

        int[] workloads = memberships.stream()
                .mapToInt(membership -> toInt(
                        completedCounts.byId().getOrDefault(membership.getUser().getId(), 0L)))
                .toArray();

        Double gini = GiniCalculator.calculate(workloads);
        Integer balance = GiniCalculator.toBalance(gini);

        FairnessInfo fairness = new FairnessInfo(gini, balance, FORMULA, generateInterpretation(balance));

        List<Zone> zones = zoneRepository.findByHousehold_Id(householdId);
        Map<UUID, Long> completedByZone =
                toCountMap(taskRepository.countCompletedByZoneInPeriod(householdId, periodStart, periodEnd));
        Map<UUID, Long> overdueByZone = toCountMap(taskRepository.countOverdueByZone(householdId, now));

        List<ZoneStats> perZone = zones.stream()
                .map(zone -> new ZoneStats(
                        zone.getId(),
                        zone.getName(),
                        toInt(completedByZone.getOrDefault(zone.getId(), 0L)),
                        toInt(overdueByZone.getOrDefault(zone.getId(), 0L))))
                .toList();

        List<Task> overdueTasks = taskRepository.findTopOverdueTasks(householdId, now, PageRequest.of(0, 5));
        List<OverdueTask> overdueTop = overdueTasks.stream()
                .map(task -> new OverdueTask(
                        task.getId(),
                        task.getTitle(),
                        safeDisplayName(
                                task.getAssignee() != null ? task.getAssignee().getDisplayName() : null),
                        toDaysOverdue(task.getDeadline(), now)))
                .toList();

        return new AnalyticsSummaryResponse(
                householdId, resolvedPeriod, periodStart, periodEnd, perMember, perZone, fairness, overdueTop);
    }

    private Instant calculatePeriodStart(String period, Instant now) {
        return switch (period) {
            case "30d" -> now.minus(30, ChronoUnit.DAYS);
            default -> now.minus(7, ChronoUnit.DAYS);
        };
    }

    private String generateInterpretation(Integer balance) {
        if (balance == null) {
            return "N/A — no tasks completed in this period";
        }
        if (balance >= 90) {
            return "Excellent balance — tasks evenly distributed among members.";
        }
        if (balance >= 70) {
            return "Good balance — workload reasonably distributed.";
        }
        if (balance >= 50) {
            return "Moderate imbalance — some members completed more tasks than others.";
        }
        if (balance >= 30) {
            return "Significant imbalance — workload concentrated on fewer members.";
        }
        return "Severe imbalance — most tasks completed by one or two members.";
    }

    private String safeDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return UNASSIGNED_NAME;
        }
        return name;
    }

    private int toDaysOverdue(Instant deadline, Instant now) {
        long days = ChronoUnit.DAYS.between(deadline, now);
        return toInt(Math.max(1L, days));
    }

    private int toInt(long value) {
        return Math.toIntExact(value);
    }

    private Counts toCounts(List<Object[]> results) {
        Map<UUID, Long> byId = new HashMap<>();
        long unassigned = 0;
        for (Object[] row : results) {
            UUID key = (UUID) row[0];
            Number count = (Number) row[1];
            long value = count != null ? count.longValue() : 0L;
            if (key == null) {
                unassigned += value;
            } else {
                byId.put(key, value);
            }
        }
        return new Counts(byId, unassigned);
    }

    private Map<UUID, Long> toCountMap(List<Object[]> results) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : results) {
            UUID key = (UUID) row[0];
            if (key == null) {
                continue;
            }
            Number count = (Number) row[1];
            map.put(key, count != null ? count.longValue() : 0L);
        }
        return map;
    }

    private record Counts(Map<UUID, Long> byId, long unassigned) {}
}
