package com.hogaria.notification.dto;

import java.util.List;

public record UpdateNotificationSettingRequest(
        List<String> channels,
        List<String> frequencies
) {
}
