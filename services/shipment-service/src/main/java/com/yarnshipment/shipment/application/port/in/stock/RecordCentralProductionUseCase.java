package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Merkez fabrikanın ürettiği kumaşı girer ve tükettiği ipliği fire ile merkez stoktan düşer. */
public interface RecordCentralProductionUseCase {

    CentralProduction recordCentralProduction(Command command);

    /**
     * Merkez kumaş girişi isteği; kumaş kilosu iplik paylarının toplamıdır.
     *
     * @param actor kaydeden (SEC-04)
     * @param fabricType kumaş cinsi
     * @param fireRate fire oranı, 0 ile 0,20 arası
     * @param rollCount top/rulo sayısı, isteğe bağlı
     * @param waybillNo iç numara, isteğe bağlı
     * @param yarns kumaşın yapıldığı iplikler
     */
    record Command(
            Actor actor,
            String fabricType,
            BigDecimal fireRate,
            @Nullable Integer rollCount,
            @Nullable String waybillNo,
            List<YarnShare> yarns) {

        public Command {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(fabricType, "fabricType");
            Objects.requireNonNull(fireRate, "fireRate");
            yarns = List.copyOf(yarns);
        }
    }
}
