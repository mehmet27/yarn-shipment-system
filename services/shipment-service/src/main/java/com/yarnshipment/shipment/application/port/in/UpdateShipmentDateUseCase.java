package com.yarnshipment.shipment.application.port.in;

/** Müdür taslak sevkiyatın gününü günceller. */
public interface UpdateShipmentDateUseCase {

    void updateShipmentDate(UpdateShipmentDateCommand command);
}
