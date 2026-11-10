package com.yarnshipment.shipment.application.port.in;

/** Teslim bekleyen sevkiyatı irsaliye numarasıyla tamamlar ve kilitler. */
public interface CompleteShipmentUseCase {

    void completeShipment(CompleteShipmentCommand command);
}
