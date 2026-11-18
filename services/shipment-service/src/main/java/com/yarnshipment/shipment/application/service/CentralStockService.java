package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockOverview;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.GetCentralStockUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import com.yarnshipment.shipment.domain.stock.ProductShare;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merkez iplik stoku: fasoncuya sevkiyat tamamlanınca düşer (defter kancası) ve merkez fabrikada
 * üretilen kumaş tükettiği ipliği fire ile düşer. Stok eksiye düşse de kayıt yapılır (doğrudan fasoncuya giden
 * ipliğin girişi henüz yapılmamış olabilir); ekran eksi stoğu uyarır.
 */
@Service
public class CentralStockService implements RecordCentralProductionUseCase, GetCentralStockUseCase {

    private static final int RECENT_LIMIT = 50;

    private final CentralStockPort stock;
    private final ActiveYarnProducts products;
    private final LedgerEventPort events;
    private final Clock clock;

    public CentralStockService(CentralStockPort stock, LedgerEventPort events, Clock clock) {
        this.stock = stock;
        this.products = new ActiveYarnProducts(stock);
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CentralProduction recordCentralProduction(RecordCentralProductionUseCase.Command command) {
        List<ProductShare> shares = products.toShares(command.yarns());
        BigDecimal netKg = shares.stream().map(ProductShare::netKg).reduce(Kilograms.ZERO, BigDecimal::add);
        Instant at = clock.instant();
        CentralProduction production = new CentralProduction(
                UUID.randomUUID(),
                command.fabricType(),
                netKg,
                command.fireRate(),
                command.rollCount(),
                command.waybillNo(),
                at,
                shares,
                command.actor().userId());
        List<CentralStockMovement> consumptions = production.consumptions();
        requireStockFor(consumptions);
        stock.saveProduction(production, consumptions);
        events.centralProductionRecorded(production);
        return production;
    }

    /** Üretim yalnızca stokta girişi olan ve yetecek kadar bulunan iplikten yapılır. */
    private void requireStockFor(List<CentralStockMovement> consumptions) {
        Map<UUID, BigDecimal> available = new HashMap<>();
        stock.findStock().stream()
                .filter(row -> row.productId() != null)
                .forEach(row -> available.merge(row.productId(), row.stockKg(), BigDecimal::add));
        Map<UUID, BigDecimal> needed = new LinkedHashMap<>();
        Map<UUID, String> names = new HashMap<>();
        for (CentralStockMovement consumption : consumptions) {
            UUID productId = Objects.requireNonNull(consumption.productId(), "productId");
            needed.merge(productId, consumption.netKg().negate(), BigDecimal::add);
            names.put(productId, consumption.yarnType());
        }
        needed.forEach((productId, kg) -> {
            BigDecimal have = available.getOrDefault(productId, Kilograms.ZERO);
            if (have.compareTo(kg) < 0) {
                throw new InsufficientCentralStockException(names.getOrDefault(productId, productId.toString()), have);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CentralStockOverview getCentralStock(Actor actor) {
        List<CentralStockRow> rows = stock.findStock();
        BigDecimal total = rows.stream().map(CentralStockRow::stockKg).reduce(Kilograms.ZERO, BigDecimal::add);
        return new CentralStockOverview(
                rows,
                total,
                stock.findProducedFabricKg(),
                stock.findYarnFlow(),
                stock.findRecent(RECENT_LIMIT),
                stock.findSupplierReceipts());
    }
}
