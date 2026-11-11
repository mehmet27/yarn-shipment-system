package com.yarnshipment.shipment.application.port.in.settings;

import com.yarnshipment.shipment.domain.model.ShipmentSettings;

/** Varsayılan fire oranı ve kantar toleransını okur. */
public interface GetShipmentSettingsUseCase {

    ShipmentSettings getSettings();
}
