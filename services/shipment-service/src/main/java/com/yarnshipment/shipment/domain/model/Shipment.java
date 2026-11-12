package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyat aggregate'i. Durum yalnızca bu tipin metotlarıyla değişir (DOM-13).
 *
 * @param id sevkiyat kimliği (UUIDv7)
 * @param shipmentNo insan okunur numara
 * @param siteId çıkış lokasyonu (GEN-07)
 * @param status durum
 * @param parties fasoncu, teslim alan, araç
 * @param planned beklenen yük
 * @param dispatchWeighing geçerli çıkış tartımı; henüz girilmediyse {@code null}
 * @param waybillNo tamamlarken girilen irsaliye numarası
 * @param createdAt oluşturma zamanı
 * @param recipe reçete satırları, fire oranı ve son kantar karşılaştırması
 */
public record Shipment(
        UUID id,
        String shipmentNo,
        UUID siteId,
        ShipmentStatus status,
        ShipmentParties parties,
        PlannedLoad planned,
        @Nullable Weighing dispatchWeighing,
        @Nullable String waybillNo,
        Instant createdAt,
        RecipeState recipe) {

    /**
     * Reçete tarafı.
     *
     * @param lines reçete satırları
     * @param fireRate fire oranı (0.04 = %4)
     * @param weightCheck son kantar karşılaştırması; uyuşmazlık olmadıysa {@code null}
     */
    public record RecipeState(
            List<RecipeLine> lines,
            BigDecimal fireRate,
            @Nullable WeightCheck weightCheck) {

        /** Reçetesiz (eski) kayıt: varsayılan fire oranı, uyuşmazlık yok. */
        public static final RecipeState LEGACY =
                new RecipeState(List.of(), ShipmentSettings.DEFAULTS.defaultFireRate(), null);

        public RecipeState {
            Objects.requireNonNull(lines, "lines");
            Objects.requireNonNull(fireRate, "fireRate");
            lines = List.copyOf(lines);
        }
    }

    public Shipment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentNo, "shipmentNo");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(parties, "parties");
        Objects.requireNonNull(planned, "planned");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(recipe, "recipe");
    }

    public List<RecipeLine> lines() {
        return recipe.lines();
    }

    public BigDecimal fireRate() {
        return recipe.fireRate();
    }

    public @Nullable WeightCheck weightCheck() {
        return recipe.weightCheck();
    }

    public UUID subcontractorId() {
        return parties.subcontractorId();
    }

    /** Çuvallar yalnızca çıkış tartımından önce (DRAFT) atanabilir. */
    public void requireBagsAssignable() {
        if (status != ShipmentStatus.DRAFT) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.AWAITING_DELIVERY);
        }
    }

    /** Depo çıkış tartımını kaydetti (DRAFT → AWAITING_DELIVERY). Kantar farkı denetlenmez. */
    public DispatchRecording recordDispatch(DispatchMeasurement measurement, Instant recordedAt) {
        return recordDispatch(measurement, recordedAt, null);
    }

    /**
     * Çıkış tartımı; beklenen net biliniyorsa ve fark {@code toleranceKg}'yi aşıyorsa sevkiyat WEIGHT_MISMATCH olur
     * (süreç analizi 5.4). {@code toleranceKg} boşsa karşılaştırma yapılmaz.
     */
    public DispatchRecording recordDispatch(
            DispatchMeasurement measurement, Instant recordedAt, @Nullable BigDecimal toleranceKg) {
        if (status != ShipmentStatus.DRAFT) {
            throw new InvalidShipmentStatusException(id, status, ShipmentStatus.AWAITING_DELIVERY);
        }
        Weighing weighing =
                new Weighing(measurement.grossKg(), measurement.tareKg(), measurement.bagCount(), recordedAt);
        WeightCheck check = null;
        BigDecimal expected = planned.netKg();
        if (toleranceKg != null && expected != null) {
            WeightCheck candidate = new WeightCheck(
                    measurement.tareKg(), measurement.grossKg(), expected, toleranceKg, measurement.shortfallReason());
            check = candidate.exceedsTolerance() ? candidate : null;
        }
        return new DispatchRecording(id, siteId, parties.subcontractorId(), weighing, recordedAt, check);
    }
}
