package com.hometusk.routines.domain;

import java.util.List;
import java.util.UUID;

public record RoundRobinState(UUID lastAssignedUserId, List<UUID> memberOrder) {}
