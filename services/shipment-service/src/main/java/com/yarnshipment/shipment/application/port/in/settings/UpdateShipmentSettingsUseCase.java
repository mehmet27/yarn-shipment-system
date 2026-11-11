package com.yarnshipment.shipment.application.port.in.settings;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;

/** Varsayılan fire oranı ve kantar toleransını günceller (yönetici); değişiklik denetim kaydına düşer. */
public interface UpdateShipmentSettingsUseCase {

    ShipmentSettings updateSettings(Actor actor, ShipmentSettings settings);
}
