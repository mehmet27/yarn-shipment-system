package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Fire dosyası portu. */
public interface FireSettlementPort {

    /** Dosyayı açar; aynı sevkiyat için zaten varsa dokunmaz. */
    void open(FireSettlement settlement, Instant at);

    Optional<FireSettlement> find(UUID shipmentId);

    /** Satırı yazma kilidiyle okur; eşzamanlı kumaş girişleri sırayla işlenir. */
    Optional<FireSettlement> lock(UUID shipmentId);

    /** Durum, oran ve fatura alanlarını yazar. */
    void save(FireSettlement settlement, Instant at);

    /** Kayıtlardan D, I ve K toplamları. */
    FireTotals totals(UUID shipmentId);

    void recordRateChange(RateChange change);

    /** Fire dosyası satırları, en yeni önce. */
    List<ListedSettlement> findSettlements(ListFireSettlementsQuery query);

    long countSettlements(ListFireSettlementsQuery query);

    /**
     * Kayıtlardan toplamlar.
     *
     * @param returnedKg D, dönen iplik
     * @param claimKg I, haklı itiraz
     * @param fabricKg K, gelen kumaş
     */
    record FireTotals(BigDecimal returnedKg, BigDecimal claimKg, BigDecimal fabricKg) {}

    /** Fire oranı değişikliğinin denetim kaydı. */
    record RateChange(
            UUID shipmentId,
            BigDecimal oldRate,
            BigDecimal newRate,
            String reason,
            UUID changedBy,
            Instant changedAt) {}

    /** Liste satırı için dosya, sevkiyat numarası, fasoncu unvanı ve toplamlar. */
    record ListedSettlement(
            FireSettlement settlement, String shipmentNo, String subcontractorTitle, FireTotals totals) {}
}
