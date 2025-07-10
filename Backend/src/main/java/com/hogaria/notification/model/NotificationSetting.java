package com.hogaria.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_type"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventTypeEnum eventType;

    @ElementCollection(targetClass = ChannelEnum.class)
    @CollectionTable(
            name = "notification_setting_channels",
            joinColumns = @JoinColumn(name = "notification_setting_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private List<ChannelEnum> channels;

    @ElementCollection(targetClass = NotificationFrequencyEnum.class)
    @CollectionTable(
            name = "notification_setting_frequencies",
            joinColumns = @JoinColumn(name = "notification_setting_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private List<NotificationFrequencyEnum> frequencies;
}
