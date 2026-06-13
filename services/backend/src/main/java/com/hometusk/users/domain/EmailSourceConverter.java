package com.hometusk.users.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailSourceConverter implements AttributeConverter<EmailSource, String> {

    @Override
    public String convertToDatabaseColumn(EmailSource attribute) {
        return (attribute == null ? EmailSource.UNKNOWN : attribute).getValue();
    }

    @Override
    public EmailSource convertToEntityAttribute(String dbData) {
        return EmailSource.fromValue(dbData);
    }
}
