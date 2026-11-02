package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancySettingsView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.TimeLimitVersionView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ToleranceVersionView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.ToleranceView;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancySettings;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.SettingVersion;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/** Ayarları web modeline çevirir; yüzdeler iki ondalıklı dize olarak taşınır (API-03). */
final class SettingsViews {

    private static final int PCT_SCALE = 2;

    private SettingsViews() {}

    static DiscrepancySettingsView toView(DiscrepancySettings settings) {
        return new DiscrepancySettingsView(
                new ToleranceView(
                        pct(settings.tolerance().greenMaxPct()),
                        pct(settings.tolerance().yellowMaxPct())),
                settings.timeLimit().inTransitMaxHours(),
                settings.toleranceHistory().stream()
                        .map(SettingsViews::toleranceVersion)
                        .toList(),
                settings.timeLimitHistory().stream()
                        .map(SettingsViews::timeLimitVersion)
                        .toList());
    }

    private static ToleranceVersionView toleranceVersion(SettingVersion<ToleranceBands> version) {
        return new ToleranceVersionView(
                        pct(version.value().greenMaxPct()),
                        pct(version.value().yellowMaxPct()),
                        utc(version.validFrom()),
                        version.setBy())
                .validTo(nullableUtc(version.validTo()));
    }

    private static TimeLimitVersionView timeLimitVersion(SettingVersion<TimeLimit> version) {
        return new TimeLimitVersionView(version.value().inTransitMaxHours(), utc(version.validFrom()), version.setBy())
                .validTo(nullableUtc(version.validTo()));
    }

    private static String pct(BigDecimal value) {
        return value.setScale(PCT_SCALE, RoundingMode.UNNECESSARY).toPlainString();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static @Nullable OffsetDateTime nullableUtc(@Nullable Instant instant) {
        return instant == null ? null : utc(instant);
    }
}
