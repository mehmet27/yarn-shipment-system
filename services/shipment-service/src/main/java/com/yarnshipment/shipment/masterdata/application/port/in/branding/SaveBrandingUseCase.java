package com.yarnshipment.shipment.masterdata.application.port.in.branding;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;

/** Marka ayarını kaydeder; yetki uçtadır (SETTINGS_MANAGE), iz audit_local trigger'ından düşer. */
public interface SaveBrandingUseCase {

    Branding saveBranding(Actor actor, Branding branding);
}
