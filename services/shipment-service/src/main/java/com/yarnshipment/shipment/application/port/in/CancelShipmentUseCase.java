package com.yarnshipment.shipment.application.port.in;

/** Yola çıkmamış sevkiyatı iptal eder ve çuvallarını serbest bırakır. */
public interface CancelShipmentUseCase {

    void cancelShipment(CancelShipmentCommand command);
}
