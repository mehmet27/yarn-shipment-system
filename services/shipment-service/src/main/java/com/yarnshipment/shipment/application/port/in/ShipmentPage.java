package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.Shipment;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    /**
     * Liste satırı.
     *
     * @param shipment sevkiyat
     * @param allowedActions isteyenin şu an yapabileceği işlemler
     */
    public record ShipmentListItem(Shipment shipment, Set<ShipmentAction> allowedActions) {

        public ShipmentListItem {
            Objects.requireNonNull(shipment, "shipment");
            allowedActions = Set.copyOf(allowedActions);
        }
    }
}
