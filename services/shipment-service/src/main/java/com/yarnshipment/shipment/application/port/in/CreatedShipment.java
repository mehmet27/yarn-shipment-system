package com.yarnshipment.shipment.application.port.in;

import java.util.UUID;

/**
 * Oluşturulan sevkiyat taslağı.
 *
 * @param id sevkiyat kimliği
 * @param shipmentNo insan okunur numara
 */
public record CreatedShipment(UUID id, String shipmentNo) {}
