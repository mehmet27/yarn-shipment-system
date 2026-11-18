package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.YarnFlow;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockEntry;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.ReceiptDetails;
import com.yarnshipment.shipment.domain.stock.SupplierReceipt;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Testler için bellek içi merkez stok defteri. */
final class InMemoryCentralStock implements CentralStockPort {
    final Map<UUID, BigDecimal> kg = new LinkedHashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    final List<CentralProduction> productions = new ArrayList<>();
    final List<ReceiptDetails> receiptDetails = new ArrayList<>();
    final Map<UUID, String> suppliers = new HashMap<>();
    private final Map<UUID, String> active;
    private List<CentralStockRow> shortages = List.of();

    InMemoryCentralStock(Map<UUID, String> active) {
        this.active = active;
    }

    void givenShortages(List<CentralStockRow> rows) {
        this.shortages = rows;
    }

    @Override
    public List<CentralStockRow> findShortagesAfterDispatch(UUID shipmentId) {
        return shortages;
    }

    BigDecimal stockOf(UUID product) {
        return kg.getOrDefault(product, BigDecimal.ZERO);
    }

    @Override
    public void saveReceipt(CentralStockMovement receipt, @Nullable ReceiptDetails details) {
        receiptDetails.add(details);
        names.put(receipt.productId(), receipt.yarnType());
        kg.merge(receipt.productId(), receipt.netKg(), BigDecimal::add);
    }

    @Override
    public void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions) {
        productions.add(production);
        consumptions.forEach(movement -> kg.merge(movement.productId(), movement.netKg(), BigDecimal::add));
    }

    @Override
    public List<CentralStockRow> findStock() {
        return kg.entrySet().stream()
                .map(entry ->
                        new CentralStockRow(entry.getKey(), names.getOrDefault(entry.getKey(), ""), entry.getValue()))
                .toList();
    }

    @Override
    public Map<UUID, String> findActiveProductNames(Collection<UUID> productIds) {
        Map<UUID, String> found = new HashMap<>();
        productIds.stream().filter(active::containsKey).forEach(id -> found.put(id, active.get(id)));
        return found;
    }

    @Override
    public YarnFlow findYarnFlow() {
        return new YarnFlow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    public List<CentralStockEntry> findRecent(int limit) {
        return List.of();
    }

    @Override
    public Optional<String> findActiveSupplierName(UUID supplierId) {
        return Optional.ofNullable(suppliers.get(supplierId));
    }

    @Override
    public List<SupplierReceipt> findSupplierReceipts() {
        return List.of();
    }

    @Override
    public BigDecimal findProducedFabricKg() {
        return BigDecimal.ZERO;
    }
}
