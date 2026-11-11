package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Shared fixtures that keep service tests below the class coupling limit. */
final class ServiceTestSupport {

    static final int BAG_COUNT = 40;

    private ServiceTestSupport() {}

    static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    static ScopedShipmentLoader loader(InMemoryShipmentQueryPort queryPort, Clock clock) {
        return new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock));
    }

    static void saveTolerance(StubSettingsPort settings, String toleranceKg) {
        settings.save(
                new ShipmentSettings(ShipmentSettings.DEFAULTS.defaultFireRate(), decimal(toleranceKg)),
                UUID.randomUUID());
    }

    static DispatchMeasurement measurement(String gross, @Nullable String shortfallReason) {
        return shortfallReason == null
                ? new DispatchMeasurement(decimal(gross), decimal("18.000"), BAG_COUNT)
                : new DispatchMeasurement(decimal(gross), decimal("18.000"), BAG_COUNT, shortfallReason);
    }

    static RecordDispatchCommand dispatchCommand(
            com.yarnshipment.common.security.Actor actor, UUID key, DispatchMeasurement measurement) {
        return new RecordDispatchCommand(actor, com.yarnshipment.shipment.TestShipments.SHIPMENT_ID, key, measurement);
    }
}
