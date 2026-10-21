package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Sevkiyat okuma portu; persistence adapter'ı uygular (PERS-08). */
public interface ShipmentQueryPort {

    Optional<Shipment> findShipment(UUID shipmentId);

    /** Kapsamdaki sevkiyatlar, en yeni önce. Kapsam filtresi WHERE koşuluna çevrilir, bellekte elenmez. */
    ShipmentSlice findShipments(AccessScope scope, Set<ShipmentStatus> statuses, PageRequest page);

    /**
     * Sayfa isteği.
     *
     * @param number sayfa numarası, 0'dan başlar
     * @param size sayfa boyu
     */
    record PageRequest(int number, int size) {}

    /**
     * Sorgu sonucu.
     *
     * @param shipments bu sayfadaki sevkiyatlar
     * @param totalElements toplam kayıt
     */
    record ShipmentSlice(List<Shipment> shipments, long totalElements) {

        public ShipmentSlice {
            shipments = List.copyOf(shipments);
        }
    }
}
