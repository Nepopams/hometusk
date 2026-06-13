package com.hometusk.notifications.service;

import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.event.TaskAssignedEvent;
import com.hometusk.users.domain.User;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TaskAssignmentNotificationService {

    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public TaskAssignmentNotificationService(
            NotificationService notificationService, ApplicationEventPublisher eventPublisher) {
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    public void notifyTaskAssigned(Task task, User actor, UUID correlationId) {
        if (task == null || task.getAssignee() == null) {
            return;
        }

        notificationService.notifyTaskAssigned(task, actor, correlationId);
        eventPublisher.publishEvent(TaskAssignedEvent.from(task, actor, correlationId));
    }
}
