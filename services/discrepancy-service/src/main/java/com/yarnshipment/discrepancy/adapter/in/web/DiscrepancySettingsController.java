package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.discrepancy.adapter.in.web.api.DiscrepancySettingsApi;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancySettingsView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.UpdateDiscrepancySettingsRequest;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancySettingsUseCase;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsCommand;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsUseCase;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Admin'in uyuşmazlık kuralı ayarları. Method security proxy'si gerektirdiği için final değildir (SEC-02). */
@RestController
public class DiscrepancySettingsController implements DiscrepancySettingsApi {

    private final GetDiscrepancySettingsUseCase getSettings;
    private final UpdateDiscrepancySettingsUseCase updateSettings;
    private final CurrentActorProvider currentActorProvider;

    public DiscrepancySettingsController(
            GetDiscrepancySettingsUseCase getSettings,
            UpdateDiscrepancySettingsUseCase updateSettings,
            CurrentActorProvider currentActorProvider) {
        this.getSettings = getSettings;
        this.updateSettings = updateSettings;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<DiscrepancySettingsView> getDiscrepancySettings() {
        return ResponseEntity.ok(SettingsViews.toView(getSettings.getSettings()));
    }

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<DiscrepancySettingsView> updateDiscrepancySettings(UpdateDiscrepancySettingsRequest request) {
        UpdateDiscrepancySettingsCommand command = new UpdateDiscrepancySettingsCommand(
                currentActorProvider.getCurrentActor(),
                new BigDecimal(request.getGreenMaxPct()),
                new BigDecimal(request.getYellowMaxPct()),
                request.getInTransitMaxHours());
        return ResponseEntity.ok(SettingsViews.toView(updateSettings.updateSettings(command)));
    }
}
