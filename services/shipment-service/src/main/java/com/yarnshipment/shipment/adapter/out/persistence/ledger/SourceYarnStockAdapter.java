package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.SourceYarnStockPort;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link SourceYarnStockPort}'un JDBC uygulaması. Defterde iplik, tanım kimliği ya da ad olarak yazılabildiği için
 * ikisi de aynı iplik sayılır (merkez stok eşlemesiyle aynı kural: büyük-küçük harf ve baş/son boşluk önemsizdir).
 */
@Component
final class SourceYarnStockAdapter implements SourceYarnStockPort {

    private static final String PRODUCT_NAME = """
            SELECT name FROM md_product WHERE id = :productId""";
    private static final String BALANCE = """
            SELECT coalesce(sum(m.net_kg), 0) FROM balance_movement m
            WHERE m.subcontractor_id = :subcontractorId
              AND (m.yarn_type = :productKey
                   OR lower(btrim(m.yarn_type)) = lower(btrim(CAST(:name AS text)))
                   OR m.yarn_type IN (SELECT id::text FROM md_product
                                      WHERE lower(btrim(name)) = lower(btrim(CAST(:name AS text)))))""";

    private final JdbcClient jdbc;

    SourceYarnStockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public YarnAvailability availability(UUID subcontractorId, @Nullable UUID productId, @Nullable String yarnText) {
        String name = yarnText;
        if (productId != null) {
            name = jdbc.sql(PRODUCT_NAME)
                    .param("productId", productId)
                    .query(String.class)
                    .optional()
                    .orElse(yarnText);
        }
        BigDecimal kg = jdbc.sql(BALANCE)
                .param("subcontractorId", subcontractorId)
                .param("productKey", productId == null ? "" : productId.toString())
                .param("name", name)
                .query(BigDecimal.class)
                .single();
        return new YarnAvailability(name == null ? "" : name, kg);
    }
}
