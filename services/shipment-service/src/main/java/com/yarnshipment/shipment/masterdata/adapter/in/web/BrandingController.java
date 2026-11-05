package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.BrandingApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.BrandingSaveRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.BrandingView;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.GetBrandingUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.SaveBrandingUseCase;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marka ayarı uçları: okuma girişsizdir (yol {@code yarnshipment.security.public-paths} içindedir, niyet
 * {@code permitAll()} ile belgelenir), yazma yalnızca SETTINGS_MANAGE. SEC-02: final değildir.
 */
@RestController
public class BrandingController implements BrandingApi {

    private final GetBrandingUseCase get;
    private final SaveBrandingUseCase save;
    private final CurrentActorProvider currentActorProvider;

    public BrandingController(
            GetBrandingUseCase get, SaveBrandingUseCase save, CurrentActorProvider currentActorProvider) {
        this.get = get;
        this.save = save;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("permitAll()")
    public ResponseEntity<BrandingView> getBranding() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(get.getBranding()));
    }

    @Override
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<BrandingView> saveBranding(BrandingSaveRequest request) {
        Branding saved = save.saveBranding(
                currentActorProvider.getCurrentActor(),
                new Branding(request.getCompanyName(), request.getPrimaryColor(), request.getLogoDataUrl()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(saved));
    }

    private static BrandingView view(Branding branding) {
        return new BrandingView(branding.primaryColor())
                .companyName(branding.companyName())
                .logoDataUrl(branding.logoDataUrl());
    }
}
