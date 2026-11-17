package com.yarnshipment.shipment.application.port.in.transport;

/** Müdür tamamlanmamış sevkiyatın şoför ve araç bilgisini düzeltir. */
public interface UpdateShipmentTransportUseCase {

    void updateShipmentTransport(UpdateShipmentTransportCommand command);
}
