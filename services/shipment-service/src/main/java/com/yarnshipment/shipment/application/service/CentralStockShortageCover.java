package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.CompleteShipmentCommand;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.CentralStockShortfallException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Sevkiyat tamamlanırken merkez stoğun eksiye düşmesini önler: uyarı verir ya da onayla eksiği giriş olarak yazar. */
@Component
final class CentralStockShortageCover {

    private final CentralStockPort stock;
    private final LedgerEventPort events;

    CentralStockShortageCover(CentralStockPort stock, LedgerEventPort events) {
        this.stock = stock;
        this.events = events;
    }

    /**
     * Merkez stok eksiye düşemez. Yetmiyorsa müdür onaylamadan tamamlama geri alınır (uyarı);
     * onaylarsa eksik kilo kadar sistem girişi yazılır, stok sıfırda kalır.
     */
    void cover(UUID shipmentId, CompleteShipmentCommand command, Instant at) {
        stock.lockForDeduction();
        List<CentralStockRow> shortages = stock.findShortagesAfterDispatch(shipmentId);
        if (shortages.isEmpty()) {
            return;
        }
        if (!command.acceptStockShortage()) {
            throw new CentralStockShortfallException(shortages.stream()
                    .map(row -> new CentralStockShortfallException.Shortfall(
                            Objects.requireNonNull(row.productId(), "productId"),
                            row.yarnType(),
                            row.stockKg().negate()))
                    .toList());
        }
        for (CentralStockRow row : shortages) {
            CentralStockMovement receipt = CentralStockMovement.receipt(
                    new CentralStockMovement.Receipt(
                            Objects.requireNonNull(row.productId(), "productId"),
                            row.yarnType(),
                            row.stockKg().negate(),
                            "Sistem girişi: sevkiyat tamamlanırken merkez stok yetmedi"),
                    at,
                    command.actor().userId());
            stock.saveReceipt(receipt);
            events.yarnReceiptRecorded(receipt);
        }
    }
}
