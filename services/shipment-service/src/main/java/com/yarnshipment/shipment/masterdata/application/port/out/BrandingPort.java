package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import java.util.Optional;
import java.util.UUID;

/** Marka ayarı okuma ve yazma portu. Yazma, değişikliği yapan kullanıcıyı transaction'a işler (audit). */
public interface BrandingPort {

    Optional<Branding> findBranding();

    /** Tek kaydı ekler, varsa günceller. */
    Branding upsertBranding(UUID actorId, Branding branding);
}
