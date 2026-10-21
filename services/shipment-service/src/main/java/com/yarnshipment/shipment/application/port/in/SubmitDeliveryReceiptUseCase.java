package com.yarnshipment.shipment.application.port.in;

/** Fasoncu teslim tutanağını gönderir; sevkiyat kontrole geçer. */
public interface SubmitDeliveryReceiptUseCase {

    void submitDeliveryReceipt(SubmitDeliveryReceiptCommand command);
}
