package com.hometusk.mobile.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMobileDeviceRequest(
        @Size(min = 8, max = 512) String pushToken,
        @Pattern(regexp = "active|inactive", message = "status must be active or inactive") String status,
        @Size(max = 40) String appVersion,
        @Size(max = 35) String locale,
        @Size(max = 80) String timezone) {}
