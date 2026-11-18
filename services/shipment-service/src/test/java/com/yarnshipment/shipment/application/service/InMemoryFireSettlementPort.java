package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.fire.InvoiceEntry;
import com.yarnshipment.shipment.application.port.in.fire.ListFireSettlementsQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort;
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
final class InMemoryFireSettlementPort implements FireSettlementPort, FireInvoicePort {

    private final Map<UUID, FireSettlement> byShipment = new HashMap<>();
    private final List<RateChange> rateChanges = new ArrayList<>();
    private final List<InvoiceRecord> recorded = new ArrayList<>();
    private Map<String, BigDecimal> dispatched = Map.of();
    private FireTotals totals = new FireTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    void givenTotals(String returnedKg, String claimKg, String fabricKg) {
        totals = new FireTotals(new BigDecimal(returnedKg), new BigDecimal(claimKg), new BigDecimal(fabricKg));
    }

    void givenInvoiced(String invoicedKg) {
        totals = new FireTotals(
                totals.returnedKg(),
                totals.claimKg(),
                totals.fabricKg(),
                new BigDecimal(invoicedKg),
                totals.pendingClaimKg(),
                totals.claimInvoicedKg());
    }

    /** Karara bağlanmış ve faturayla bakiyeden düşülecek haklı itiraz kilosu; fire tabanı için de sayılır. */
    void givenPendingClaim(String claimKg) {
        totals = new FireTotals(
                totals.returnedKg(),
                new BigDecimal(claimKg),
                totals.fabricKg(),
                totals.invoicedKg(),
                new BigDecimal(claimKg),
                totals.claimInvoicedKg());
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
    public Map<String, BigDecimal> dispatchedByYarn(UUID shipmentId) {
        return dispatched;
    }

    @Override
    public boolean invoiceExists(UUID shipmentId, String invoiceNo) {
        return recorded.stream()
                .anyMatch(
                        i -> i.shipmentId().equals(shipmentId) && i.invoiceNo().equals(invoiceNo));
    }

    @Override
    public void recordInvoice(InvoiceRecord invoice) {
        recorded.add(invoice);
        BigDecimal kg = invoice.lines().stream().map(l -> l.netKg()).reduce(BigDecimal.ZERO, BigDecimal::add);
        totals = new FireTotals(
                totals.returnedKg(),
                totals.claimKg(),
                totals.fabricKg(),
                totals.invoicedKg().add(kg),
                totals.pendingClaimKg(),
                totals.claimInvoicedKg().add(invoice.claimKg()));
    }

    @Override
    public List<InvoiceEntry> invoices(UUID shipmentId) {
        return recorded.stream()
                .map(i -> new InvoiceEntry(
                        i.invoiceNo(),
                        i.invoicedOn(),
                        i.lines().stream().map(l -> l.netKg()).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
    }

    List<InvoiceRecord> recorded() {
        return recorded;
    }

    void givenDispatched(Map<String, BigDecimal> byYarn) {
        dispatched = byYarn;
    }

    @Override
    public long countSettlements(ListFireSettlementsQuery query) {
        return byShipment.size();
    }
}
