package com.yarnshipment.discrepancy.application.port.in;

/**
 * Tolerans bantlarını ve süre sınırını değiştirir. Yeni değer kaydedildiği andan sonraki karşılaştırmalara ve
 * kontrollere uygulanır; açılmış uyuşmazlıklar değişmez.
 */
public interface UpdateDiscrepancySettingsUseCase {

    DiscrepancySettings updateSettings(UpdateDiscrepancySettingsCommand command);
}
