package com.yarnshipment.shipment.application.port.out.ledger;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Gönderen fasoncunun elindeki iplik (bakiye defterinin toplamı). */
public interface SourceYarnStockPort {

    /**
     * Fasoncunun bu iplikteki bakiyesi. İplik, tanım kimliğiyle ya da serbest metinle verilir; defterde kimlikle ya da
     * ad olarak yazılmış hareketlerin ikisi de sayılır. Hareket yoksa sıfırdır; ad, tanım adı ya da verilen metindir.
     */
    YarnAvailability availability(UUID subcontractorId, @Nullable UUID productId, @Nullable String yarnText);

    /** İplik adı ve fasoncunun elindeki kilo (eksi olabilir). */
    record YarnAvailability(String yarnType, BigDecimal kg) {}
}
