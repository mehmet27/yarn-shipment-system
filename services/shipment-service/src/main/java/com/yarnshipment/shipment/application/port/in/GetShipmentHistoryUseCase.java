package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.shipment.domain.model.StatusChange;
import java.util.List;

/** Kapsamdaki sevkiyatın durum geçmişini eskiden yeniye döner. */
public interface GetShipmentHistoryUseCase {

    /**
     * Durum geçmişi, eskiden yeniye.
     *
     * @throws ShipmentNotFoundException kayıt yoksa (404)
     * @throws com.yarnshipment.common.security.scope.OutOfScopeAccessException kapsam dışındaysa (403)
     */
    List<StatusChange> getHistory(GetShipmentHistoryQuery query);
}
