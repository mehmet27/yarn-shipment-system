package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.GetBrandingUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.branding.SaveBrandingUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.BrandingPort;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marka ayarını okur ve kaydeder. Yazma yetkisi uçta verilir. */
@Service
public class BrandingService implements GetBrandingUseCase, SaveBrandingUseCase {

    private final BrandingPort branding;

    public BrandingService(BrandingPort branding) {
        this.branding = branding;
    }

    @Override
    @Transactional(readOnly = true)
    public Branding getBranding() {
        return branding.findBranding().orElse(Branding.DEFAULT);
    }

    @Override
    @Transactional
    public Branding saveBranding(Actor actor, Branding value) {
        return branding.upsertBranding(actor.userId(), value);
    }
}
