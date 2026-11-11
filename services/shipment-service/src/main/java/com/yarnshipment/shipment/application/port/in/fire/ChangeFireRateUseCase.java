package com.yarnshipment.shipment.application.port.in.fire;

/** Sevkiyatın fire oranını değiştirir (FIRE_RATE_EDIT); kapanmış dosyada reddedilir. */
public interface ChangeFireRateUseCase {

    FireSettlementDetails changeFireRate(ChangeFireRateCommand command);
}
