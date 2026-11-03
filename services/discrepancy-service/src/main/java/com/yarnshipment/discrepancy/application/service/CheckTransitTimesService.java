package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.CheckTransitTimesUseCase;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyOpeningPort;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.application.port.out.TimeLimitRulePort;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.rule.TimeChecks;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sınırdan uzun süredir yolda olan sevkiyatlar için {@code TIME_IN_TRANSIT_EXCEEDED} açar (eksik listesi 3.14). Her
 * sevkiyatta bir kez açılır; sevkiyat sonra varsa da uyuşmazlık açık kalır ve teslimde sevkiyatı incelemeye alır. Bir
 * çalışmada en çok {@link #BATCH_SIZE} sevkiyat işlenir, kalanı sonraki çalışmaya kalır.
 */
@Service
public class CheckTransitTimesService implements CheckTransitTimesUseCase {

    static final int BATCH_SIZE = 200;

    private final ShipmentWatchPort watchPort;
    private final TimeLimitRulePort timeLimits;
    private final DiscrepancyOpeningPort openingPort;
    private final Clock clock;

    public CheckTransitTimesService(
            ShipmentWatchPort watchPort,
            TimeLimitRulePort timeLimits,
            DiscrepancyOpeningPort openingPort,
            Clock clock) {
        this.watchPort = watchPort;
        this.timeLimits = timeLimits;
        this.openingPort = openingPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int checkTransitTimes() {
        Instant now = clock.instant();
        TimeLimit limit = timeLimits.findTimeLimit(now).orElse(TimeLimit.REPORT_DEFAULT);
        Instant dispatchedBefore = now.minus(Duration.ofHours(limit.inTransitMaxHours()));
        int opened = 0;
        for (WatchedShipment shipment : watchPort.findInTransitSince(dispatchedBefore, BATCH_SIZE)) {
            Instant dispatchedAt = Objects.requireNonNull(shipment.dispatchedAt());
            Optional<Finding> finding = TimeChecks.inTransit(dispatchedAt, now, limit);
            if (finding.isPresent() && watchPort.markTransitAlerted(shipment.shipmentId(), now)) {
                openingPort.openDiscrepancy(new OpenedDiscrepancy(
                        shipment, finding.get(), now, SystemDefaults.dueAt(now), SystemDefaults.SYSTEM_ACTOR));
                opened++;
            }
        }
        return opened;
    }
}
