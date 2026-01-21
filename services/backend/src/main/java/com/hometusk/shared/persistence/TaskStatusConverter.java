package com.hometusk.shared.persistence;

import com.hometusk.tasks.domain.TaskStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskStatusConverter extends LowercaseEnumConverter<TaskStatus> {

    public TaskStatusConverter() {
        super(TaskStatus.class);
    }
}
