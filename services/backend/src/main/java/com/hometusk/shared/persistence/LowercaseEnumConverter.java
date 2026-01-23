package com.hometusk.shared.persistence;

import jakarta.persistence.AttributeConverter;
import java.util.Locale;

public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> enumType;

    protected LowercaseEnumConverter(Class<E> enumType) {
        this.enumType = enumType;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute != null ? attribute.name().toLowerCase(Locale.ROOT) : null;
    }

    @Override
    public E convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Enum.valueOf(enumType, dbData.toUpperCase(Locale.ROOT));
    }
}
