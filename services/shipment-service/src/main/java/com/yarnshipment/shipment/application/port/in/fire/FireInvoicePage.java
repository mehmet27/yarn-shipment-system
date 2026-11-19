package com.yarnshipment.shipment.application.port.in.fire;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sayfalı faturalananlar listesi (API-06).
 *
 * @param items bu sayfadaki faturalar
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 * @param totalKg süzgece uyan tüm faturaların toplam kilosu (yalnız bu sayfa değil)
 */
public record FireInvoicePage(List<Row> items, int number, int size, long totalElements, BigDecimal totalKg) {

    public FireInvoicePage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /** Liste satırı: bir fatura ve girildiği sevkiyat. */
    public record Row(
            String invoiceNo,
            LocalDate invoicedOn,
            UUID shipmentId,
            String shipmentNo,
            UUID subcontractorId,
            String subcontractorTitle,
            BigDecimal kg) {}
}
