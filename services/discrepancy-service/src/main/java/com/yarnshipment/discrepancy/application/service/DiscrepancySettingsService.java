package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.DiscrepancySettings;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancySettingsUseCase;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsCommand;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsUseCase;
import com.yarnshipment.discrepancy.application.port.out.TimeLimitRulePort;
import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin'in tolerans bantlarını (eksik listesi 4.5) ve yolda kalma süresi sınırını ayarlaması. Yeni değer eski kuralın
 * geçerliliği kapatılıp yeni satırla yazılır (geçmiş korunur); yalnızca değişen kural yazılır. Yetki kontrolü
 * ({@code SETTINGS_MANAGE}) uçtadır.
 */
@Service
public class DiscrepancySettingsService implements GetDiscrepancySettingsUseCase, UpdateDiscrepancySettingsUseCase {

    /** Ekranda gösterilen geçmiş sürüm sayısı. */
    static final int HISTORY_SIZE = 10;

    private final ToleranceRulePort toleranceRules;
    private final TimeLimitRulePort timeLimits;
    private final Clock clock;

    public DiscrepancySettingsService(ToleranceRulePort toleranceRules, TimeLimitRulePort timeLimits, Clock clock) {
        this.toleranceRules = toleranceRules;
        this.timeLimits = timeLimits;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DiscrepancySettings getSettings() {
        return read(clock.instant());
    }

    @Override
    @Transactional
    public DiscrepancySettings updateSettings(UpdateDiscrepancySettingsCommand command) {
        ToleranceBands bands = ToleranceBands.ofSetting(command.greenMaxPct(), command.yellowMaxPct());
        TimeLimit limit = new TimeLimit(command.inTransitMaxHours());
        Instant now = clock.instant();
        DiscrepancySettings current = read(now);
        if (!sameBands(current.tolerance(), bands)) {
            toleranceRules.replaceGeneralRule(bands, command.actor().userId(), now);
        }
        if (!current.timeLimit().equals(limit)) {
            timeLimits.replaceTimeLimit(limit, command.actor().userId(), now);
        }
        return read(now);
    }

    private DiscrepancySettings read(Instant at) {
        return new DiscrepancySettings(
                toleranceRules.findGeneralRule(at).orElse(ToleranceBands.REPORT_DEFAULT),
                timeLimits.findTimeLimit(at).orElse(TimeLimit.REPORT_DEFAULT),
                toleranceRules.findGeneralRuleHistory(HISTORY_SIZE),
                timeLimits.findTimeLimitHistory(HISTORY_SIZE));
    }

    /** {@code 0.5} ile {@code 0.50} aynı banttır. */
    private static boolean sameBands(ToleranceBands left, ToleranceBands right) {
        return left.greenMaxPct().compareTo(right.greenMaxPct()) == 0
                && left.yellowMaxPct().compareTo(right.yellowMaxPct()) == 0;
    }
}
