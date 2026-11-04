package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fasoncu bazında gönderilen, teslim alınan ve yolda kalan kilo. İptal edilen sevkiyatlar sayılmaz.
 *
 * @param subcontractorId fasoncu
 * @param shipmentCount iptal edilmemiş sevkiyat
 * @param dispatchedKg çıkış net toplamı
 * @param deliveredKg teslim net toplamı
 * @param inTransitKg henüz teslim kaydı olmayan sevkiyatların çıkış net toplamı
 */
public record SubcontractorBalance(
        UUID subcontractorId,
        int shipmentCount,
        BigDecimal dispatchedKg,
        BigDecimal deliveredKg,
        BigDecimal inTransitKg) {

    /** Teslim edilmiş sevkiyatlarda çıkış − teslim: fasonda ya da yolda kaybolan miktar (fire). */
    public BigDecimal differenceKg() {
        return dispatchedKg.subtract(inTransitKg).subtract(deliveredKg);
    }
}
