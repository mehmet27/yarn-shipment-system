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
     * @param netKg gelen net kg
     * @param note not, isteğe bağlı
     */
    record Command(
            Actor actor,
            UUID productId,
            BigDecimal netKg,
            @Nullable String note) {

        public Command {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(netKg, "netKg");
        }
    }
}
