package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ShipmentSettingsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentSettingsRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.ShipmentSettingsView;
import com.yarnshipment.shipment.application.port.in.settings.GetShipmentSettingsUseCase;
import com.yarnshipment.shipment.application.port.in.settings.UpdateShipmentSettingsUseCase;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import java.math.BigDecimal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Varsayılan fire oranı ve kantar toleransı uçları; yazma yalnızca SETTINGS_MANAGE. SEC-02: final değildir. */
@RestController
public class ShipmentSettingsController implements ShipmentSettingsApi {

    private final GetShipmentSettingsUseCase getSettings;
    private final UpdateShipmentSettingsUseCase updateSettings;
    private final CurrentActorProvider currentActorProvider;

    public ShipmentSettingsController(
            GetShipmentSettingsUseCase getSettings,
            UpdateShipmentSettingsUseCase updateSettings,
            CurrentActorProvider currentActorProvider) {
        this.getSettings = getSettings;
        this.updateSettings = updateSettings;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SHIPMENT_VIEW')")
    public ResponseEntity<ShipmentSettingsView> getShipmentSettings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(getSettings.getSettings()));
    }

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<ShipmentSettingsView> saveShipmentSettings(ShipmentSettingsRequest request) {
        ShipmentSettings saved = updateSettings.updateSettings(
                currentActorProvider.getCurrentActor(),
                new ShipmentSettings(
                        new BigDecimal(request.getDefaultFireRate()), new BigDecimal(request.getWeightToleranceKg())));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(saved));
    }

    private static ShipmentSettingsView view(ShipmentSettings value) {
        return new ShipmentSettingsView()
                .defaultFireRate(WebMappings.fireRate(value.defaultFireRate()))
                .weightToleranceKg(WebMappings.kg(value.weightToleranceKg()));
    }
}
