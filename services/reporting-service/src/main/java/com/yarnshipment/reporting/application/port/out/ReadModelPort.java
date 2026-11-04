package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.ReadModelChange;
import com.yarnshipment.reporting.domain.model.ShipmentParties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Okuma modelini yazar. Tüm yazımlar çağıranın transaction'ındadır. */
public interface ReadModelPort {

    void insertShipment(ReadModelChange.ShipmentCreated created);

    /** Sevkiyat okuma modelinde varsa {@code true}. */
    boolean saveDispatched(ReadModelChange.ShipmentDispatched dispatched);

    boolean saveArrived(ReadModelChange.ShipmentArrived arrived);

    boolean saveCompleted(ReadModelChange.ShipmentCompleted completed);

    boolean saveCancelled(ReadModelChange.ShipmentCancelled cancelled);

    boolean saveDelivery(ReadModelChange.DeliveryReceived delivery);

    boolean saveComparison(ReadModelChange.ComparisonCompleted comparison);

    Optional<ShipmentParties> findParties(UUID shipmentId);

    /** Günlük eşleşme sayaçlarına bir karşılaştırma ekler. */
    void addPairComparison(PairComparison comparison);

    void insertDiscrepancy(ReadModelChange.DiscrepancyOpened opened);

    void saveDiscrepancyResolved(ReadModelChange.DiscrepancyResolved resolved);

    /**
     * Bir karşılaştırmanın eşleşme sayaçlarına katkısı.
     *
     * @param comparedAt karşılaştırma zamanı
     * @param day Europe/Istanbul günü
     * @param siteId lokasyon
     * @param driverId şoför
     * @param subcontractorId fasoncu
     * @param discrepancyCount açılan uyuşmazlık sayısı
     * @param absoluteDiffKg mutlak kilo farkı
     */
    record PairComparison(
            Instant comparedAt,
            LocalDate day,
            UUID siteId,
            UUID driverId,
            UUID subcontractorId,
            int discrepancyCount,
            BigDecimal absoluteDiffKg) {}
}
