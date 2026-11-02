package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.common.security.Actor;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Admin'in girdiği yeni ayarlar; değişmeyen kural yeniden yazılmaz.
 *
 * @param actor ayarı değiştiren Admin
 * @param greenMaxPct bu yüzdeye kadar Yeşil
 * @param yellowMaxPct bu yüzdeye kadar Sarı, üstü Kırmızı
 * @param inTransitMaxHours sevkiyat en çok bu kadar saat yolda kalabilir
 */
public record UpdateDiscrepancySettingsCommand(
        Actor actor, BigDecimal greenMaxPct, BigDecimal yellowMaxPct, int inTransitMaxHours) {

    public UpdateDiscrepancySettingsCommand {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(greenMaxPct, "greenMaxPct");
        Objects.requireNonNull(yellowMaxPct, "yellowMaxPct");
    }
}
