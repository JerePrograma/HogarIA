package com.hogaria.notification.service;

import com.hogaria.notification.dto.NotificationSettingDto;
import com.hogaria.notification.dto.UpdateNotificationSettingRequest;

import java.util.List;

public interface NotificationSettingService {
    List<NotificationSettingDto> listByUser(Long userId);

    NotificationSettingDto update(Long id, UpdateNotificationSettingRequest dto);
}
