package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tutar bilgisi. Her sevkiyat satırının birim fiyatı farklı olduğu için tutarlar satır satır (kilo × o satırın fiyatı)
 * hesaplanıp toplanır; tek bir fiyatla çarpılmaz.
 *
 * @param shippedAmount dönemde yola çıkan iplik tutarı
 * @param shippedUnpricedKg fiyatı girilmemiş (eski kayıt) yola çıkan kilo; tutara katılmaz
 * @param toInvoiceAmount faturalanacak eksik kilonun tutarı
 * @param toInvoiceUnpricedKg fiyatı bilinmediği için tutarı hesaplanamayan faturalanacak kilo
 * @param subcontractors fasoncuda kalan iplik kilosu ve tahmini değeri
 */
public record AmountSummary(
        BigDecimal shippedAmount,
        BigDecimal shippedUnpricedKg,
        BigDecimal toInvoiceAmount,
        BigDecimal toInvoiceUnpricedKg,
        List<SubcontractorAmount> subcontractors) {

    /** Fasoncuda kalan iplik toplam değeri. */
    public BigDecimal subcontractorYarnAmount() {
        return subcontractors.stream().map(SubcontractorAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Fiyatı bulunamadığı için değeri hesaplanamayan fasoncudaki iplik kilosu. */
    public BigDecimal subcontractorUnpricedKg() {
        return subcontractors.stream().map(SubcontractorAmount::unpricedKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
