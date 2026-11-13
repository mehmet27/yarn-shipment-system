package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fire.ChangeFireRateCommand;
import com.yarnshipment.shipment.application.port.in.fire.ChangeFireRateUseCase;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort.RateChange;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sevkiyatın fire oranını değiştirir (KR-02). Değişiklik gerekçesiyle denetim kaydına düşer; kapanmış dosyada
 * reddedilir. Dosya kilitlenerek eşzamanlı kumaş girişleriyle sıraya girer.
 */
@Service
public class FireRateService implements ChangeFireRateUseCase {

    private final ScopedShipmentLoader shipments;
    private final FireSettlementPort settlements;
    private final FireSettlementDetailsReader reader;
    private final LedgerEventPort events;
    private final Clock clock;

    public FireRateService(
            ScopedShipmentLoader shipments,
            FireSettlementPort settlements,
            FireSettlementDetailsReader reader,
            LedgerEventPort events,
            Clock clock) {
        this.shipments = shipments;
        this.settlements = settlements;
        this.reader = reader;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FireSettlementDetails changeFireRate(ChangeFireRateCommand command) {
        shipments.load(command.actor(), command.shipmentId(), Permission.FIRE_RATE_EDIT);
        FireSettlement current = settlements
                .lock(command.shipmentId())
                .orElseThrow(() -> new FireSettlementNotFoundException(command.shipmentId()));
        String reason = command.reason().strip();
        if (reason.isEmpty()) {
            throw new InvalidLedgerInputException("reason");
        }
        FireSettlement changed = current.withFireRate(command.fireRate());
        Instant now = clock.instant();
        settlements.save(changed, now);
        RateChange change = new RateChange(
                current.shipmentId(), current.fireRate(), changed.fireRate(), reason, command.actor().userId(), now);
        settlements.recordRateChange(change);
        events.fireRateChanged(change);
        return reader.read(changed);
    }
}
