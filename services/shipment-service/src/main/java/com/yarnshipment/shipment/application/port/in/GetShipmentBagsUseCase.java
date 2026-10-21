package com.yarnshipment.shipment.application.port.in;

/** Kapsamdaki sevkiyatın çuvallarını isteyenin görünümüyle (kör giriş, SEC-05) döner. */
public interface GetShipmentBagsUseCase {

    /**
     * Çuval listeleri, isteyenin görünümüne göre.
     *
     * @throws ShipmentNotFoundException kayıt yoksa (404)
     * @throws com.yarnshipment.common.security.scope.OutOfScopeAccessException kapsam dışındaysa (403)
     */
    ShipmentBags getBags(GetShipmentBagsQuery query);
}
