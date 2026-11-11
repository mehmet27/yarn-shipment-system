package com.yarnshipment.shipment.application.port.in.fire;

/** Sevkiyatın fire dosyasını ve hesabını okur. */
public interface GetFireSettlementUseCase {

    FireSettlementDetails getFireSettlement(FireSettlementQuery query);
}
