package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.util.List;

/**
 * Admin'in ayarladığı uyuşmazlık kuralları ve geçmişleri.
 *
 * @param tolerance güncel tolerans bantları
 * @param timeLimit güncel durumda kalma süresi sınırı
 * @param toleranceHistory tolerans sürümleri, en yenisi önce
 * @param timeLimitHistory süre sınırı sürümleri, en yenisi önce
 */
public record DiscrepancySettings(
        ToleranceBands tolerance,
        TimeLimit timeLimit,
        List<SettingVersion<ToleranceBands>> toleranceHistory,
        List<SettingVersion<TimeLimit>> timeLimitHistory) {

    public DiscrepancySettings {
        toleranceHistory = List.copyOf(toleranceHistory);
        timeLimitHistory = List.copyOf(timeLimitHistory);
    }
}
