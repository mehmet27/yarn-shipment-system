package com.yarnshipment.shipment.masterdata.application.port.in.branding;

import com.yarnshipment.shipment.masterdata.domain.branding.Branding;

/** Marka ayarını okur; hiç kaydedilmemişse varsayılanı verir. Giriş sayfası da okuduğu için yetki istemez. */
public interface GetBrandingUseCase {

    Branding getBranding();
}
