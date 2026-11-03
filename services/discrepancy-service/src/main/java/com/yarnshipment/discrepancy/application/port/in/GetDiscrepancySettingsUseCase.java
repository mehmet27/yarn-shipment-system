package com.yarnshipment.discrepancy.application.port.in;

/** Uyuşmazlık kurallarının güncel ayarları; kural tanımlı değilse rapordaki varsayılanlar. */
public interface GetDiscrepancySettingsUseCase {

    DiscrepancySettings getSettings();
}
