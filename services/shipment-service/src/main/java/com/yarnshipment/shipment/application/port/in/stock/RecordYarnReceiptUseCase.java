package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Merkeze gelen ipliği stoğa yazar (CENTRAL_STOCK_RECORD). */
public interface RecordYarnReceiptUseCase {

    CentralStockMovement recordYarnReceipt(Command command);

    /**
     * Merkeze iplik girişi isteği.
     *
     * @param actor kaydeden (SEC-04)
     * @param productId iplik tanımı (Tanımlar'daki aktif iplik)
     * @param supplierId iplik tedarikçisi (Tanımlar'daki aktif tedarikçi)
     * @param netKg gelen net kg
     * @param lotNo tedarikçinin parti numarası, isteğe bağlı; sevkiyat ve iplik dönüşündeki parti buradan gelir
     * @param qualityGradeId parti ilk kez yazılıyorsa partinin kalite sınıfı
     * @param waybillNo tedarikçi irsaliye numarası, isteğe bağlı
     * @param note not, isteğe bağlı
     */
    record Command(
            Actor actor,
            UUID productId,
            UUID supplierId,
            BigDecimal netKg,
            @Nullable String lotNo,
            @Nullable UUID qualityGradeId,
            @Nullable String waybillNo,
            @Nullable String note) {

        public Command {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(supplierId, "supplierId");
            Objects.requireNonNull(netKg, "netKg");
        }
    }
}
