package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.fabric.ListYarnReturnsUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.RecordYarnReturnCommand;
import com.yarnshipment.shipment.application.port.in.fabric.RecordYarnReturnUseCase;
import com.yarnshipment.shipment.application.port.in.fabric.ShipmentRecordsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.application.port.out.ledger.YarnReturnPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.ReturnDirection;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dönen iplik ve fasoncudan gelen iplik (analiz 5.9). İkisi de bakiyeden düşer; dönen iplik ayrıca fire tabanından
 * düşer (KR-14) ve gönderilen kiloyu aşamaz. Yalnız tamamlanmış sevkiyata ve kapanmamış fire dosyasına girilir.
 */
@Service
public class YarnReturnService implements RecordYarnReturnUseCase, ListYarnReturnsUseCase {

    private final ScopedShipmentLoader shipments;
    private final YarnReturnPort yarnReturns;
    private final FireSettlementPort settlements;
    private final Clock clock;

    public YarnReturnService(
            ScopedShipmentLoader shipments, YarnReturnPort yarnReturns, FireSettlementPort settlements, Clock clock) {
        this.shipments = shipments;
        this.yarnReturns = yarnReturns;
        this.settlements = settlements;
        this.clock = clock;
    }

    @Override
    @Transactional
    public YarnReturn recordYarnReturn(RecordYarnReturnCommand command) {
        Shipment shipment = shipments.load(command.actor(), command.shipmentId(), Permission.YARN_RETURN_RECORD);
        if (shipment.status() != ShipmentStatus.COMPLETED) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.COMPLETED);
        }
        FireSettlement settlement =
                settlements.lock(shipment.id()).orElseThrow(() -> new FireSettlementNotFoundException(shipment.id()));
        settlement.requireOpen();
        YarnReturn record = new YarnReturn(
                UUID.randomUUID(),
                shipment.id(),
                shipment.subcontractorId(),
                command.direction(),
                command.yarnType(),
                command.lotId(),
                command.netKg(),
                command.waybillNo(),
                clock.instant(),
                command.actor().userId());
        if (record.direction() == ReturnDirection.RETURN) {
            requireWithinSentKg(settlement, record);
        }
        yarnReturns.save(record);
        return record;
    }

    @Override
    @Transactional(readOnly = true)
    public List<YarnReturn> listYarnReturns(ShipmentRecordsQuery query) {
        shipments.load(query.actor(), query.shipmentId(), Permission.SHIPMENT_VIEW);
        return yarnReturns.findByShipment(query.shipmentId());
    }

    /** Dönen iplik toplamı gönderilen kiloyu (haklı itiraz düşülmüş) aşamaz. */
    private void requireWithinSentKg(FireSettlement settlement, YarnReturn record) {
        FireSettlementPort.FireTotals totals = settlements.totals(settlement.shipmentId());
        boolean exceeds = totals.returnedKg()
                        .add(record.netKg())
                        .compareTo(settlement.sentKg().subtract(totals.claimKg()))
                > 0;
        if (exceeds) {
            throw new InvalidLedgerInputException("netKg");
        }
    }
}
