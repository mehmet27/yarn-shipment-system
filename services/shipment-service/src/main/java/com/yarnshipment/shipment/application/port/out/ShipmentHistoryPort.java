package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;
import java.util.UUID;

/** Durum geçmişi okuma portu; geçmiş yalnızca eklenir (DOM-14). */
public interface ShipmentHistoryPort {

    /** Eskiden yeniye sıralı geçişler. */
    List<StatusChange> findHistory(UUID shipmentId);
}
