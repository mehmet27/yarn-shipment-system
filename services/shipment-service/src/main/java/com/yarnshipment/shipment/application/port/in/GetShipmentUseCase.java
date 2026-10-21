package com.yarnshipment.shipment.application.port.in;

/** Tek sevkiyatı kapsam kontrolünden geçirip isteyenin görünümüyle döner. */
public interface GetShipmentUseCase {

    /**
     * Sevkiyat ayrıntısı.
     *
     * @throws ShipmentNotFoundException kayıt yoksa (404)
     * @throws com.yarnshipment.common.security.scope.OutOfScopeAccessException kapsam dışındaysa (403)
     */
    ShipmentDetails getShipment(GetShipmentQuery query);
}
