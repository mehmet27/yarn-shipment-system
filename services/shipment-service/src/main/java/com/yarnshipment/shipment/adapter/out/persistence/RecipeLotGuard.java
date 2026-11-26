package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Reçete satırının partisi o satırın ipliğine ait olmalıdır; merkez iplik girişindeki kuralın aynısı. */
final class RecipeLotGuard {

    private static final String OTHER_YARN = """
            SELECT EXISTS (SELECT 1 FROM md_production_lot WHERE id = :lotId AND product_id <> :productId)""";

    private RecipeLotGuard() {}

    static void require(JdbcClient jdbc, RecipeLine line) {
        UUID lotId = line.lotId();
        UUID productId = line.yarnTypeId();
        if (lotId == null || productId == null) {
            return;
        }
        boolean isOtherYarn = Boolean.TRUE.equals(jdbc.sql(OTHER_YARN)
                .param("lotId", lotId)
                .param("productId", productId)
                .query(Boolean.class)
                .single());
        if (isOtherYarn) {
            throw new InvalidNewShipmentException("lines.lotId", "lot belongs to another yarn");
        }
    }
}
