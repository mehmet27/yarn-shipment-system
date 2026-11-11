package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kantar uyuşmazlığı kararının yazımı: karar kaydı, durum geçişi, reçete güncellemesi ve olay tek transaction'da. */
public interface WeightDecisionPort {

    /** Açık (kararsız) uyuşmazlık kaydına kararı yazar. */
    void recordDecision(UUID shipmentId, WeightDecision decision, String reason, Audit audit);

    /** WEIGHT_MISMATCH → DRAFT. Arada durum değiştiyse {@code InvalidShipmentStatusException}. */
    void backToDraft(UUID shipmentId, String reason, Audit audit);

    /**
     * Reçete satırlarını ve beklenen net'i tartıya eşitler, WEIGHT_MISMATCH → AWAITING_DELIVERY geçirir ve
     * çıkış olayını yazar.
     */
    void acceptRecipe(RecipeAcceptance acceptance, String reason, Audit audit);

    /** Kararı veren kullanıcı ve an. */
    record Audit(UUID actorUserId, Instant at) {}

    /** Reçetenin tartıya eşitlenmesi: ölçülen net, ölçeklenmiş satırlar ve yazılacak çıkış olayı. */
    record RecipeAcceptance(
            UUID shipmentId, List<RecipeLine> scaledLines, BigDecimal measuredKg, ShipmentDispatched event) {}
}
