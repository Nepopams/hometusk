package com.hometusk.shared.persistence;

import com.hometusk.commands.domain.CommandType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommandTypeConverter extends LowercaseEnumConverter<CommandType> {

    public CommandTypeConverter() {
        super(CommandType.class);
    }
}
