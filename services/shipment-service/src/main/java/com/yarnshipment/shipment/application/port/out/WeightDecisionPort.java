package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.time.Instant;
import java.util.UUID;

/** Kantar uyuşmazlığı kararının yazımı: karar kaydı ve durum geçişi tek transaction'da. */
public interface WeightDecisionPort {

    /** Açık (kararsız) uyuşmazlık kaydına kararı yazar. */
    void recordDecision(UUID shipmentId, WeightDecision decision, String reason, Audit audit);

    /** WEIGHT_MISMATCH → DRAFT. Arada durum değiştiyse {@code InvalidShipmentStatusException}. */
    void backToDraft(UUID shipmentId, String reason, Audit audit);

    /** Kararı veren kullanıcı ve an. */
    record Audit(UUID actorUserId, Instant at) {}
}
