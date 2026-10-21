package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.DeliveryCommandPort;
import com.yarnshipment.shipment.domain.model.DeliveryReceipt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Gönderilen tutanakları bellekte tutar. */
final class RecordingDeliveryCommandPort implements DeliveryCommandPort {

    private final List<DeliveryReceipt> receipts = new ArrayList<>();

    List<DeliveryReceipt> receipts() {
        return receipts;
    }

    @Override
    public boolean hasDeliveryReceipt(UUID shipmentId) {
        return receipts.stream().anyMatch(receipt -> receipt.shipmentId().equals(shipmentId));
    }

    @Override
    public void recordDelivery(DeliveryReceipt receipt) {
        receipts.add(receipt);
    }
}
