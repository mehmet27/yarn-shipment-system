package com.yarnshipment.shipment.application.port.in.driver;

/** Şoför yükü teslim alır; sevkiyat yola çıkar. */
public interface ConfirmPickupUseCase {

    void confirmPickup(ConfirmPickupCommand command);
}
