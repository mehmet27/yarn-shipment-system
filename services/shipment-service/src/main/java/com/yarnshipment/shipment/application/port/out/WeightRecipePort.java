package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Düzeltilen reçetenin yazımı: satırlar, beklenen net, durum geçişi ve çıkış olayı tek transaction'da. */
public interface WeightRecipePort {

    /**
     * Mevcut satırları sırayla günceller, fazla satırları ekler, beklenen net'i satır toplamına eşitler,
     * WEIGHT_MISMATCH → AWAITING_DELIVERY geçirir ve çıkış olayını yazar. Arada durum değiştiyse
     * {@code InvalidShipmentStatusException}.
     */
    void replaceAndDispatch(RecipeReplacement replacement, String reason, WeightDecisionPort.Audit audit);

    /** Yeni reçete: satırlar, toplamı ve yazılacak çıkış olayı. */
    record RecipeReplacement(UUID shipmentId, List<RecipeLine> lines, BigDecimal totalKg, ShipmentDispatched event) {}
}
