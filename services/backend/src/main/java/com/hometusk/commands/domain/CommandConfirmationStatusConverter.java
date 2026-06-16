package com.hometusk.commands.domain;

import com.hometusk.shared.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CommandConfirmationStatusConverter extends LowercaseEnumConverter<CommandConfirmationStatus> {

    public CommandConfirmationStatusConverter() {
        super(CommandConfirmationStatus.class);
    }
}
