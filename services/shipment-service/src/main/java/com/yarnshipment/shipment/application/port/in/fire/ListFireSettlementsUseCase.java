package com.yarnshipment.shipment.application.port.in.fire;

/** Fire dosyalarını sayfalı okur (BALANCE_VIEW). */
public interface ListFireSettlementsUseCase {

    FireSettlementPage listFireSettlements(ListFireSettlementsQuery query);
}
