package com.yarnshipment.discrepancy.application.port.in.claim;

/** Sevkiyatın itiraz süresini, teslimde okutulan çuvallarını ve itirazlarını döner. */
public interface GetShipmentClaimsUseCase {

    ShipmentClaims getShipmentClaims(GetShipmentClaimsQuery query);
}
