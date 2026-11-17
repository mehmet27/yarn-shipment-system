package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreateShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.application.port.out.ShipmentCreationPort;
import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.application.port.out.ledger.SourceYarnStockPort;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDays;
import com.yarnshipment.shipment.domain.stock.SourceYarnShortageException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sevkiyat taslağı oluşturur; fire oranı verilmezse ayardaki varsayılan kullanılır. Lokasyon, kullanıcının yetkili
 * olduğu lokasyonlardan biri olmalıdır (GEN-07). Fasoncudan fasoncuya sevkiyatta gönderen fasoncunun elinde iplik
 * yoksa taslak açılmaz.
 */
@Service
public class CreateShipmentService implements CreateShipmentUseCase {

    private static final String RESOURCE_TYPE = "Shipment";

    private final ShipmentCreationPort creationPort;
    private final ShipmentSettingsPort settingsPort;
    private final SourceYarnStockPort sourceStock;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public CreateShipmentService(
            ShipmentCreationPort creationPort,
            ShipmentSettingsPort settingsPort,
            SourceYarnStockPort sourceStock,
            ScopeGuard scopeGuard,
            Clock clock) {
        this.creationPort = creationPort;
        this.settingsPort = settingsPort;
        this.sourceStock = sourceStock;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedShipment createShipment(CreateShipmentCommand command) {
        Actor actor = command.actor();
        BigDecimal fireRate = command.fireRate() != null
                ? command.fireRate()
                : settingsPort.find().defaultFireRate();
        if (command.shipmentDate().isBefore(ShipmentDays.today(clock))) {
            throw new InvalidNewShipmentException("shipmentDate", "must not be in the past");
        }
        NewShipment shipment = new NewShipment(
                command.siteId(),
                command.parties(),
                command.vehicleId(),
                command.lines(),
                fireRate,
                command.shipmentDate(),
                clock.instant());
        ScopedResource target = new ScopedResource(RESOURCE_TYPE, UUID.randomUUID(), shipment.siteId());
        scopeGuard.validateInScope(actor, target, Permission.SHIPMENT_CREATE.name());
        requireSourceYarn(shipment);
        return creationPort.create(shipment, actor.userId());
    }

    /** Gönderen fasoncu varsa her iplik için istenen toplam kilo, onun bakiyesini aşamaz; aşıyorsa hiçbiri açılmaz. */
    private void requireSourceYarn(NewShipment shipment) {
        UUID source = shipment.parties().sourceSubcontractorId();
        if (source == null) {
            return;
        }
        Map<String, List<RecipeLine>> byYarn = new LinkedHashMap<>();
        for (RecipeLine line : shipment.lines()) {
            String key = line.yarnTypeId() != null
                    ? line.yarnTypeId().toString()
                    : String.valueOf(line.yarnTypeText()).strip().toLowerCase(Locale.ROOT);
            byYarn.computeIfAbsent(key, ignored -> new ArrayList<>()).add(line);
        }
        List<SourceYarnShortageException.Shortage> shortages = new ArrayList<>();
        for (List<RecipeLine> lines : byYarn.values()) {
            RecipeLine first = lines.getFirst();
            BigDecimal requested = lines.stream().map(RecipeLine::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
            SourceYarnStockPort.YarnAvailability available =
                    sourceStock.availability(source, first.yarnTypeId(), first.yarnTypeText());
            BigDecimal missing = requested.subtract(available.kg().max(BigDecimal.ZERO));
            if (missing.signum() > 0) {
                shortages.add(new SourceYarnShortageException.Shortage(available.yarnType(), missing));
            }
        }
        if (!shortages.isEmpty()) {
            throw new SourceYarnShortageException(shortages);
        }
    }
}
