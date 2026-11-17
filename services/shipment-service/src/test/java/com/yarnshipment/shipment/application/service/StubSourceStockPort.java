package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ledger.SourceYarnStockPort;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Gönderen fasoncunun iplik bakiyesi; iplik adına (metin) göre kilo, yoksa sıfır. */
final class StubSourceStockPort implements SourceYarnStockPort {

    private final Map<String, BigDecimal> kgByYarn = new HashMap<>();

    void set(String yarnType, String kg) {
        kgByYarn.put(yarnType, new BigDecimal(kg));
    }

    @Override
    public YarnAvailability availability(UUID subcontractorId, @Nullable UUID productId, @Nullable String yarnText) {
        String name = String.valueOf(yarnText);
        return new YarnAvailability(name, kgByYarn.getOrDefault(name, BigDecimal.ZERO));
    }
}
