package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.application.port.in.settings.GetShipmentSettingsUseCase;
import com.yarnshipment.shipment.application.port.in.settings.UpdateShipmentSettingsUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Varsayılan fire oranı ve kantar toleransı ayarları. Yetki (SETTINGS_MANAGE) web katmanında denetlenir. */
@Service
public class ShipmentSettingsService implements GetShipmentSettingsUseCase, UpdateShipmentSettingsUseCase {

    private final ShipmentSettingsPort settingsPort;

    public ShipmentSettingsService(ShipmentSettingsPort settingsPort) {
        this.settingsPort = settingsPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentSettings getSettings() {
        return settingsPort.find();
    }

    @Override
    @Transactional
    public ShipmentSettings updateSettings(Actor actor, ShipmentSettings settings) {
        settingsPort.save(settings, actor.userId());
        return settingsPort.find();
    }
}
