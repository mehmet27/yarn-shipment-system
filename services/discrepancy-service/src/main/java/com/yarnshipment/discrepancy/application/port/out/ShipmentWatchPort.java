package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sevkiyat olaylarından tutulan izleme kaydı. Olaylar tekrar ya da sırasız gelebilir: oluşturma ikinci kez yazılmaz,
 * diğer adımlar yalnızca boş alanı doldurur; kaydı olmayan sevkiyat (servisten önce açılmış) sessizce atlanır.
 */
public interface ShipmentWatchPort {

    /** Yeni sevkiyat; yola çıkış zamanı henüz yoktur. */
    void recordCreated(WatchedShipment shipment, Instant createdAt);

    void recordDispatched(UUID shipmentId, Instant dispatchedAt);

    void recordArrived(UUID shipmentId, Instant arrivedAt);

    /** Tamamlanan ya da iptal edilen sevkiyat. */
    void recordClosed(UUID shipmentId, Instant closedAt);

    Optional<WatchedShipment> findShipment(UUID shipmentId);

    /** Verilen andan önce yola çıkmış, varmamış, kapanmamış ve henüz uyarılmamış sevkiyatlar; en eskisi önce. */
    List<WatchedShipment> findInTransitSince(Instant dispatchedBefore, int limit);

    /** Yolda süre aşımı uyuşmazlığı açıldı; ikinci kez açılmaz. Başkası işaretlediyse {@code false}. */
    boolean markTransitAlerted(UUID shipmentId, Instant at);
}
