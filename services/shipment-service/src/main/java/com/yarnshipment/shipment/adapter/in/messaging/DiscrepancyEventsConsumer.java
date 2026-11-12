package com.yarnshipment.shipment.adapter.in.messaging;

import com.yarnshipment.common.outbox.DomainEventConsumer;
import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentCommand;
import com.yarnshipment.shipment.application.port.in.claim.RecordClaimAdjustmentUseCase;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Haklı bulunan kalite itirazının kilosunu bakiye defterine yansıtır; taşıyıcıdan bağımsızdır. Diğer olaylar
 * kullanılmaz ve yok sayılır.
 *
 * <p>Teslim: {@link Delivery#BEFORE_COMMIT}. Defter düzeltmesi kaybedilemez; hata olursa uyuşmazlık kararı da geri
 * alınır, karar ile defter birbirinden ayrışmaz.
 */
@Component
final class DiscrepancyEventsConsumer implements DomainEventConsumer {

    /** EVT-06: olay okuyucusu bilinmeyen alanları yok sayar. */
    private static final JsonMapper EVENT_READER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Olayda kullanıcı yoksa deftere sistem kullanıcısı yazılır. */
    private static final UUID SYSTEM_USER = new UUID(0L, 0L);

    private final RecordClaimAdjustmentUseCase claimAdjustment;

    DiscrepancyEventsConsumer(RecordClaimAdjustmentUseCase claimAdjustment) {
        this.claimAdjustment = claimAdjustment;
    }

    @Override
    public Delivery delivery() {
        return Delivery.BEFORE_COMMIT;
    }

    @Override
    public void consume(IncomingEvent event) {
        if (DiscrepancyResolved.TYPE.equals(event.type())) {
            onResolved(event);
        }
    }

    private void onResolved(IncomingEvent event) {
        DiscrepancyResolved resolved = EVENT_READER.readValue(event.data(), DiscrepancyResolved.class);
        BigDecimal claimedKg = resolved.claimedKg();
        if (claimedKg != null && claimedKg.signum() > 0) {
            String actorId = event.actorId();
            claimAdjustment.recordClaimAdjustment(new RecordClaimAdjustmentCommand(
                    resolved.discrepancyId(),
                    resolved.shipmentId(),
                    claimedKg,
                    resolved.resolvedAt(),
                    actorId == null ? SYSTEM_USER : UUID.fromString(actorId)));
        }
    }
}
