package com.hogaria.notification.dto;


import java.util.List;

public record NotificationSettingDto(
        Long id,
        Long userId,
        String eventType,
        List<String> channels,
        List<String> frequencies
) {
}
