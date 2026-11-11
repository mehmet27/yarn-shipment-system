package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import java.util.UUID;

/** Kurulum ayarları (varsayılan fire oranı, kantar toleransı). Kayıt yoksa kod varsayılanları döner. */
public interface ShipmentSettingsPort {

    ShipmentSettings find();

    /** Ayarları yazar; değişiklik {@code audit_local} izine düşer. */
    void save(ShipmentSettings settings, UUID actorUserId);
}
