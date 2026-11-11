package com.yarnshipment.shipment.application.port.in.fire;

import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import java.util.List;
import java.util.UUID;

/**
 * Sayfalı fire dosyası listesi (API-06).
 *
 * @param items bu sayfadaki satırlar
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 */
public record FireSettlementPage(List<Row> items, int number, int size, long totalElements) {

    public FireSettlementPage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    /** Liste satırı. */
    public record Row(
            UUID shipmentId,
            String shipmentNo,
            String subcontractorTitle,
            FireSettlementStatus status,
            FireCalculation calculation) {}
}
