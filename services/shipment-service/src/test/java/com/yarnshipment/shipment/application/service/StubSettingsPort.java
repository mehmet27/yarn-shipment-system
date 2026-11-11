package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import java.util.UUID;

/** Ayarları bellekte tutar. */
final class StubSettingsPort implements ShipmentSettingsPort {

    private ShipmentSettings settings = ShipmentSettings.DEFAULTS;

    StubSettingsPort() {}

    StubSettingsPort(ShipmentSettings settings) {
        this.settings = settings;
    }

    @Override
    public ShipmentSettings find() {
        return settings;
    }

    @Override
    public void save(ShipmentSettings newSettings, UUID actorUserId) {
        this.settings = newSettings;
    }
}
