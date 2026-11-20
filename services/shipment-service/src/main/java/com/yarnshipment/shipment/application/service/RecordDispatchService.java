package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.application.port.out.ShipmentBagsPort;
import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.domain.model.DispatchBag;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentDays;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.weight.BagCountMismatchException;
import com.yarnshipment.shipment.domain.weight.BagWeightMismatchException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Depo çıkış tartımı; sevkiyat teslim bekliyor durumuna geçer. Net, beklenen net'ten kantar toleransından fazla
 * saparsa sevkiyat bunun yerine WEIGHT_MISMATCH olur ve müdür karar verir. Sevkiyat DRAFT'tan çıktıysa ve tartım
 * zaten varsa ikinci istek yeni kayıt üretmez, ilk sonuç (204) tekrar döner (API-04). Tartılan çuval sayısı
 * sevkiyata atanmış çuval sayısına eşit, çuvalların kilo toplamı da araç net kilosuna (kantar toleransı içinde)
 * eşit olmalıdır; aksi halde hiçbir şey yazılmaz ve depo çuvalları kontrol eder.
 */
@Service
public class RecordDispatchService implements RecordDispatchUseCase {

    private final ScopedShipmentLoader shipments;
    private final DispatchCommandPort dispatchCommandPort;
    private final ShipmentBagsPort bagsPort;
    private final ShipmentSettingsPort settingsPort;
    private final Clock clock;

    public RecordDispatchService(
            ScopedShipmentLoader shipments,
            DispatchCommandPort dispatchCommandPort,
            ShipmentBagsPort bagsPort,
            ShipmentSettingsPort settingsPort,
            Clock clock) {
        this.shipments = shipments;
        this.dispatchCommandPort = dispatchCommandPort;
        this.bagsPort = bagsPort;
        this.settingsPort = settingsPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void recordDispatch(RecordDispatchCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.loadForUpdate(actor, command.shipmentId(), Permission.DISPATCH_RECORD);
        if (isRepeatedRequest(shipment)) {
            return;
        }
        shipment.requireDue(ShipmentDays.today(clock));
        BigDecimal tolerance = settingsPort.find().weightToleranceKg();
        if (shipment.status() == ShipmentStatus.DRAFT) {
            requireBagsMatchWeighing(shipment, command.measurement(), tolerance);
        }
        dispatchCommandPort.recordDispatch(
                shipment.recordDispatch(command.measurement(), clock.instant(), tolerance), actor.userId());
    }

    private void requireBagsMatchWeighing(Shipment shipment, DispatchMeasurement measurement, BigDecimal tolerance) {
        List<DispatchBag> bags = bagsPort.findDispatchBags(shipment.id());
        if (bags.size() != measurement.bagCount()) {
            throw new BagCountMismatchException(measurement.bagCount(), bags.size());
        }
        BigDecimal bagKg = bags.stream().map(DispatchBag::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netKg = measurement.grossKg().subtract(measurement.tareKg());
        if (bagKg.subtract(netKg).abs().compareTo(tolerance) > 0) {
            throw new BagWeightMismatchException(bagKg, netKg, tolerance);
        }
    }

    private boolean isRepeatedRequest(Shipment shipment) {
        return shipment.status() != ShipmentStatus.DRAFT && dispatchCommandPort.hasDispatchWeighing(shipment.id());
    }
}
