package com.hometusk.shared.persistence;

import com.hometusk.activity.domain.ActivityType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ActivityTypeConverter extends LowercaseEnumConverter<ActivityType> {

    public ActivityTypeConverter() {
        super(ActivityType.class);
    }
}
