package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat olaylarından tutulan izleme kaydı. Olaylar tekrar ya da sırasız gelebilir: oluşturma ikinci kez yazılmaz,
 * diğer adımlar yalnızca boş alanı doldurur; kaydı olmayan sevkiyat (servisten önce açılmış) sessizce atlanır.
 */
public interface ShipmentWatchPort {

    /** Yeni sevkiyat; yola çıkış zamanı henüz yoktur. */
    void recordCreated(WatchedShipment shipment, Instant createdAt);

    /** Çıkış tartısı girildi; net kilo kalite itirazının üst sınırıdır. */
    void recordDispatched(UUID shipmentId, Instant dispatchedAt, @Nullable BigDecimal netKg);

    /** Tamamlanan sevkiyat; kalite itiraz süresi buradan sayılır. Aynı zamanda sevkiyatı kapatır. */
    void recordCompleted(UUID shipmentId, Instant completedAt);

    /** Tamamlanan ya da iptal edilen sevkiyat. */
    void recordClosed(UUID shipmentId, Instant closedAt);

    Optional<WatchedShipment> findShipment(UUID shipmentId);
}
