package com.hogaria.domains.transactions.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.repository.MonthlyPlanItemRepository;
import com.hogaria.repository.MonthlyPlanTransactionMatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonthlyPlanTransactionLinkServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 26);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-26T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void paidFutureBecomesScheduled() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.PAID,
                TODAY.plusDays(1),
                MonthlyPlanItem.Status.SCHEDULED
        );
    }

    @Test
    void paidTodayBecomesScheduled() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.PAID,
                TODAY,
                MonthlyPlanItem.Status.SCHEDULED
        );
    }

    @Test
    void paidPastBecomesDue() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.PAID,
                TODAY.minusDays(1),
                MonthlyPlanItem.Status.DUE
        );
    }

    @Test
    void paidWithoutDateBecomesEstimated() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.PAID,
                null,
                MonthlyPlanItem.Status.ESTIMATED
        );
    }

    @Test
    void collectedFutureBecomesScheduled() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.COLLECTED,
                TODAY.plusDays(1),
                MonthlyPlanItem.Status.SCHEDULED
        );
    }

    @Test
    void collectedPastBecomesDue() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.COLLECTED,
                TODAY.minusDays(1),
                MonthlyPlanItem.Status.DUE
        );
    }

    @Test
    void pendingStatusesKeepTheirStatus() {
        for (var status : List.of(
                MonthlyPlanItem.Status.DRAFT,
                MonthlyPlanItem.Status.ESTIMATED,
                MonthlyPlanItem.Status.SCHEDULED,
                MonthlyPlanItem.Status.DUE
        )) {
            assertStatusAfterUnlink(status, TODAY.minusDays(1), status);
        }
    }

    @Test
    void cancelledKeepsItsStatus() {
        assertStatusAfterUnlink(
                MonthlyPlanItem.Status.CANCELLED,
                TODAY.minusDays(1),
                MonthlyPlanItem.Status.CANCELLED
        );
    }

    private void assertStatusAfterUnlink(
            MonthlyPlanItem.Status initialStatus,
            LocalDate expectedDate,
            MonthlyPlanItem.Status expectedStatus
    ) {
        var profileId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var item = MonthlyPlanItem.builder()
                .profileId(profileId)
                .transactionId(transactionId)
                .status(initialStatus)
                .expectedDate(expectedDate)
                .build();

        var itemRepository = mock(MonthlyPlanItemRepository.class);
        var matchRepository = mock(MonthlyPlanTransactionMatchRepository.class);
        when(itemRepository.findByProfileIdAndTransactionId(profileId, transactionId))
                .thenReturn(List.of(item));
        when(matchRepository.findByProfileIdAndMoneyTransactionId(profileId, transactionId))
                .thenReturn(List.of());

        var service = new MonthlyPlanTransactionLinkService(itemRepository, matchRepository, CLOCK);

        var result = service.unlinkTransaction(profileId, transactionId);

        assertThat(result.linkedItemsUpdated()).isEqualTo(1);
        assertThat(item.getTransactionId()).isNull();
        assertThat(item.getStatus()).isEqualTo(expectedStatus);
    }
}
