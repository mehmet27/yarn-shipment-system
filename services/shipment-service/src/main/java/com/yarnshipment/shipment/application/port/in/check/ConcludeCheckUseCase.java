package com.yarnshipment.shipment.application.port.in.check;

/** Karşılaştırma sonucuyla sevkiyatı tamamlar ya da incelemeye alır. */
public interface ConcludeCheckUseCase {

    void concludeCheck(ConcludeCheckCommand command);
}
