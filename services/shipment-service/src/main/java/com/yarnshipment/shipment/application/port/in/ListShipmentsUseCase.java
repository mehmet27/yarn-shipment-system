package com.yarnshipment.shipment.application.port.in;

/** Aktörün kapsamındaki sevkiyatları listeler; kapsam sorguya eklenir (SEC-03). */
public interface ListShipmentsUseCase {

    ShipmentPage listShipments(ListShipmentsQuery query);
}
