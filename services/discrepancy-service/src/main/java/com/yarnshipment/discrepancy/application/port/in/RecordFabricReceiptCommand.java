package com.yarnshipment.discrepancy.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sevkiyata girilen kumaş.
 *
 * @param shipmentId sevkiyat
 * @param receiptId kumaş kaydı; aynı kayıt bir kez sayılır
 * @param netKg gelen kumaş net kg
 * @param receivedAt kumaşın geliş zamanı
 */
public record RecordFabricReceiptCommand(UUID shipmentId, UUID receiptId, BigDecimal netKg, Instant receivedAt) {

    public RecordFabricReceiptCommand {
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(receiptId, "receiptId");
        Objects.requireNonNull(netKg, "netKg");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
