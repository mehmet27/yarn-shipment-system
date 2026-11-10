package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.ReadModelChange;

/** Okuma modelini yazar. Tüm yazımlar çağıranın transaction'ındadır. */
public interface ReadModelPort {

    void insertShipment(ReadModelChange.ShipmentCreated created);

    /** Sevkiyat okuma modelinde varsa {@code true}. */
    boolean saveDispatched(ReadModelChange.ShipmentDispatched dispatched);

    boolean saveCompleted(ReadModelChange.ShipmentCompleted completed);

    boolean saveCancelled(ReadModelChange.ShipmentCancelled cancelled);

    void insertDiscrepancy(ReadModelChange.DiscrepancyOpened opened);

    void saveDiscrepancyResolved(ReadModelChange.DiscrepancyResolved resolved);
}
