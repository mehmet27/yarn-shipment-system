package com.yarnshipment.shipment.application.port.in;

/** Sevkiyat taslağı oluşturma. */
public interface CreateShipmentUseCase {

    /** Lokasyon kullanıcının kapsamında değilse 403. */
    CreatedShipment createShipment(CreateShipmentCommand command);
}
