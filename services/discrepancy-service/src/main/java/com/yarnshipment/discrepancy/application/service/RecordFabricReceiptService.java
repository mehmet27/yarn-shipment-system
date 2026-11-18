package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.discrepancy.application.port.in.RecordFabricReceiptCommand;
import com.yarnshipment.discrepancy.application.port.in.RecordFabricReceiptUseCase;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Girilen kumaşı izleme kaydına yazar; olay tekrar gelirse aynı kumaş ikinci kez sayılmaz. */
@Service
public class RecordFabricReceiptService implements RecordFabricReceiptUseCase {

    private final ShipmentWatchPort watchPort;

    public RecordFabricReceiptService(ShipmentWatchPort watchPort) {
        this.watchPort = watchPort;
    }

    @Override
    @Transactional
    public void recordFabricReceipt(RecordFabricReceiptCommand command) {
        watchPort.recordFabricReceived(
                command.shipmentId(), command.receiptId(), command.netKg(), command.receivedAt());
    }
}
