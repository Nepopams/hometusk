package com.hometusk.shared.persistence;

import com.hometusk.commands.domain.CommandStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommandStatusConverter extends LowercaseEnumConverter<CommandStatus> {

    public CommandStatusConverter() {
        super(CommandStatus.class);
    }
}
