package com.hometusk.shared.persistence;

import com.hometusk.commands.domain.DecisionSource;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = true)
public class DecisionSourceConverter extends LowercaseEnumConverter<DecisionSource> {

    public DecisionSourceConverter() {
        super(DecisionSource.class);
    }

    @Override
    public DecisionSource convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        String normalized = dbData.toLowerCase(Locale.ROOT);
        if ("ai".equals(normalized)) {
            return DecisionSource.AI_PLATFORM;
        }

        return super.convertToEntityAttribute(dbData);
    }
}
