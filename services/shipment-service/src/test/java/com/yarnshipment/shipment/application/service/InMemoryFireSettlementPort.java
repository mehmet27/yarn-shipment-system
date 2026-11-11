package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FireSettlementPort;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Fire dosyası portunun bellek içi sahtesi; toplamlar testin verdiği değerlerdir. */
final class InMemoryFireSettlementPort implements FireSettlementPort {

    private final Map<UUID, FireSettlement> byShipment = new HashMap<>();
    private final List<RateChange> rateChanges = new ArrayList<>();
    private FireTotals totals = new FireTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    void givenTotals(String returnedKg, String claimKg, String fabricKg) {
        totals = new FireTotals(new BigDecimal(returnedKg), new BigDecimal(claimKg), new BigDecimal(fabricKg));
    }

    void put(FireSettlement settlement) {
        byShipment.put(settlement.shipmentId(), settlement);
    }

    List<RateChange> rateChanges() {
        return rateChanges;
    }

    @Override
    public void open(FireSettlement settlement, Instant at) {
        byShipment.putIfAbsent(settlement.shipmentId(), settlement);
    }

    @Override
    public Optional<FireSettlement> find(UUID shipmentId) {
        return Optional.ofNullable(byShipment.get(shipmentId));
    }

    @Override
    public Optional<FireSettlement> lock(UUID shipmentId) {
        return find(shipmentId);
    }

    @Override
    public void save(FireSettlement settlement, Instant at) {
        byShipment.put(settlement.shipmentId(), settlement);
    }

    @Override
    public FireTotals totals(UUID shipmentId) {
        return totals;
    }

    @Override
    public void recordRateChange(RateChange change) {
        rateChanges.add(change);
    }

    @Override
    public List<ListedSettlement> findSettlements(ListFireSettlementsQuery query) {
        return byShipment.values().stream()
                .map(settlement -> new ListedSettlement(settlement, "SV-1", "Örnek Tekstil", totals))
                .toList();
    }

    @Override
    public long countSettlements(ListFireSettlementsQuery query) {
        return byShipment.size();
    }
}
