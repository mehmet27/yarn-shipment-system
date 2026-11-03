package com.yarnshipment.discrepancy.domain.rule;

import com.yarnshipment.discrepancy.domain.model.InvalidSettingException;

/**
 * Durumda kalma süresi sınırı.
 *
 * @param inTransitMaxHours sevkiyat en çok bu kadar saat yolda kalabilir
 */
public record TimeLimit(int inTransitMaxHours) {

    public static final int MIN_HOURS = 1;
    /** Otuz gün; daha uzun sınır kuralı fiilen kapatır. */
    public static final int MAX_HOURS = 720;

    /** Kural tanımlı değilse ön analizdeki örnek: 24 saat. */
    public static final TimeLimit REPORT_DEFAULT = new TimeLimit(24);

    public TimeLimit {
        if (inTransitMaxHours < MIN_HOURS || inTransitMaxHours > MAX_HOURS) {
            throw new InvalidSettingException("inTransitMaxHours");
        }
    }
}
