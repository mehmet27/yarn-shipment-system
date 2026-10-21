package com.yarnshipment.shipment.application.port.in;

import java.util.List;

/**
 * Sayfalı sevkiyat listesi (API-06).
 *
 * @param items bu sayfadaki satırlar
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 */
public record ShipmentPage(List<ShipmentListItem> items, int number, int size, long totalElements) {

    public ShipmentPage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
