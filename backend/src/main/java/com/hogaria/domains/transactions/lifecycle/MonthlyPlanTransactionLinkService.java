package com.hogaria.domains.transactions.lifecycle;

import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;
import com.hogaria.repository.MonthlyPlanItemRepository;
import com.hogaria.repository.MonthlyPlanTransactionMatchRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonthlyPlanTransactionLinkService {

    private final MonthlyPlanItemRepository monthlyPlanItemRepository;
    private final MonthlyPlanTransactionMatchRepository matchRepository;
    private final Clock clock;

    @Autowired
    public MonthlyPlanTransactionLinkService(
            MonthlyPlanItemRepository monthlyPlanItemRepository,
            MonthlyPlanTransactionMatchRepository matchRepository
    ) {
        this(monthlyPlanItemRepository, matchRepository, Clock.systemDefaultZone());
    }

    MonthlyPlanTransactionLinkService(
            MonthlyPlanItemRepository monthlyPlanItemRepository,
            MonthlyPlanTransactionMatchRepository matchRepository,
            Clock clock
    ) {
        this.monthlyPlanItemRepository = monthlyPlanItemRepository;
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    public MonthlyPlanTransactionUnlinkResult describeLinks(UUID profileId, UUID transactionId) {
        var links = loadLinks(profileId, transactionId);

        return new MonthlyPlanTransactionUnlinkResult(
                links.linkedItems().size(),
                links.matches().size(),
                links.systemConversionMatches()
        );
    }

    public MonthlyPlanTransactionUnlinkResult unlinkTransaction(UUID profileId, UUID transactionId) {
        var links = loadLinks(profileId, transactionId);
        var linkedItems = links.linkedItems();
        var matches = links.matches();

        if (!matches.isEmpty()) {
            matchRepository.deleteAll(matches);
            matchRepository.flush();
        }

        if (!linkedItems.isEmpty()) {
            var today = LocalDate.now(clock);
            linkedItems.forEach(item -> unlinkItem(item, today));
            monthlyPlanItemRepository.saveAll(linkedItems);
            monthlyPlanItemRepository.flush();
        }

        return new MonthlyPlanTransactionUnlinkResult(
                linkedItems.size(),
                matches.size(),
                links.systemConversionMatches()
        );
    }

    private void unlinkItem(MonthlyPlanItem item, LocalDate today) {
        item.setTransactionId(null);

        if (item.getStatus() == MonthlyPlanItem.Status.PAID
                || item.getStatus() == MonthlyPlanItem.Status.COLLECTED) {
            item.setStatus(statusAfterUnlink(item, today));
        }
    }

    private MonthlyPlanItem.Status statusAfterUnlink(MonthlyPlanItem item, LocalDate today) {
        if (item.getExpectedDate() == null) {
            return MonthlyPlanItem.Status.ESTIMATED;
        }

        if (item.getExpectedDate().isBefore(today)) {
            return MonthlyPlanItem.Status.DUE;
        }

        return MonthlyPlanItem.Status.SCHEDULED;
    }

    private LinkSnapshot loadLinks(UUID profileId, UUID transactionId) {
        var linkedItems = monthlyPlanItemRepository.findByProfileIdAndTransactionId(profileId, transactionId);
        var matches = matchRepository.findByProfileIdAndMoneyTransactionId(profileId, transactionId);

        int systemConversionMatches = (int) matches.stream()
                .filter(match -> match.getMatchType() == MonthlyPlanTransactionMatch.MatchType.SYSTEM_CONVERSION)
                .count();

        return new LinkSnapshot(linkedItems, matches, systemConversionMatches);
    }

    private record LinkSnapshot(
            List<MonthlyPlanItem> linkedItems,
            List<MonthlyPlanTransactionMatch> matches,
            int systemConversionMatches
    ) {
    }
}
