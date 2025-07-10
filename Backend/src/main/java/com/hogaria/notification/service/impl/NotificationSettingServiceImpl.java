package com.hogaria.notification.service.impl;

import com.hogaria.notification.dto.NotificationSettingDto;
import com.hogaria.notification.dto.UpdateNotificationSettingRequest;
import com.hogaria.notification.model.*;
import com.hogaria.notification.repository.NotificationSettingRepository;
import com.hogaria.notification.service.NotificationSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository repo;

    private static NotificationSettingDto toDto(NotificationSetting s) {
        return new NotificationSettingDto(
                s.getId(),
                s.getUserId(),
                s.getEventType().name(),
                s.getChannels().stream().map(Enum::name).collect(Collectors.toList()),
                s.getFrequencies().stream().map(Enum::name).collect(Collectors.toList())
        );
    }

    @Override
    public List<NotificationSettingDto> listByUser(Long userId) {
        return repo.findAllByUserId(userId)
                .stream().map(NotificationSettingServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationSettingDto update(Long id, UpdateNotificationSettingRequest dto) {
        NotificationSetting existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "NotificationSetting not found"));

        existing.setChannels(
                dto.channels().stream()
                        .map(ChannelEnum::valueOf)
                        .collect(Collectors.toList())
        );
        existing.setFrequencies(
                dto.frequencies().stream()
                        .map(NotificationFrequencyEnum::valueOf)
                        .collect(Collectors.toList())
        );

        return toDto(repo.save(existing));
    }
}
