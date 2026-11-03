package com.yarnshipment.discrepancy.application.port.in;

/** Oluşturulan sevkiyatı izlemeye alır; zaman ve fotoğraf kuralları yalnızca izlenen sevkiyatlarda çalışır. */
public interface RegisterShipmentUseCase {

    void registerShipment(RegisterShipmentCommand command);
}
