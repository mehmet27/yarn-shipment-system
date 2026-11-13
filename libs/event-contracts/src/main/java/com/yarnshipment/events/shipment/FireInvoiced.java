package com.yarnshipment.events.shipment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * FireFaturaEdildi: fire dosyası "fatura edildi" olarak işaretlendi. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param invoiceNo fatura numarası
 * @param invoicedOn fatura tarihi
 * @param markedAt işaretlenme zamanı
 */
public record FireInvoiced(UUID shipmentId, String invoiceNo, LocalDate invoicedOn, Instant markedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.fire-invoiced.v1";
}
