package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockOverview;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.GetCentralStockUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.application.port.out.ledger.LedgerEventPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merkez iplik stoku: giriş eklenir, fasoncuya sevkiyat tamamlanınca düşer (defter kancası) ve merkez fabrikada
 * üretilen kumaş tükettiği ipliği fire ile düşer. Stok eksiye düşse de kayıt yapılır (doğrudan fasoncuya giden
 * ipliğin girişi henüz yapılmamış olabilir); ekran eksi stoğu uyarır.
 */
@Service
public class CentralStockService
        implements RecordYarnReceiptUseCase, RecordCentralProductionUseCase, GetCentralStockUseCase {

    private static final int RECENT_LIMIT = 50;

    private final CentralStockPort stock;
    private final LedgerEventPort events;
    private final Clock clock;

    public CentralStockService(CentralStockPort stock, LedgerEventPort events, Clock clock) {
        this.stock = stock;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CentralStockMovement recordYarnReceipt(RecordYarnReceiptUseCase.Command command) {
        CentralStockMovement receipt = CentralStockMovement.receipt(
                new CentralStockMovement.Receipt(command.yarnType(), command.netKg(), command.note()),
                clock.instant(),
                command.actor().userId());
        stock.saveReceipt(receipt);
        events.yarnReceiptRecorded(receipt);
        return receipt;
    }

    @Override
    @Transactional
    public CentralProduction recordCentralProduction(RecordCentralProductionUseCase.Command command) {
        BigDecimal netKg = command.yarns().stream().map(YarnShare::netKg).reduce(Kilograms.ZERO, BigDecimal::add);
        Instant at = clock.instant();
        CentralProduction production = new CentralProduction(
                UUID.randomUUID(),
                command.fabricType(),
                netKg,
                command.fireRate(),
                command.rollCount(),
                command.waybillNo(),
                at,
                command.yarns(),
                command.actor().userId());
        List<CentralStockMovement> consumptions = production.consumptions();
        requireStockFor(consumptions);
        stock.saveProduction(production, consumptions);
        events.centralProductionRecorded(production);
        return production;
    }

    /** Üretim yalnızca stokta girişi olan ve yetecek kadar bulunan iplikten yapılır. */
    private void requireStockFor(List<CentralStockMovement> consumptions) {
        Map<String, BigDecimal> available = new HashMap<>();
        stock.findStock().forEach(row -> available.merge(row.yarnType(), row.stockKg(), BigDecimal::add));
        Map<String, BigDecimal> needed = new LinkedHashMap<>();
        for (CentralStockMovement consumption : consumptions) {
            needed.merge(consumption.yarnType(), consumption.netKg().negate(), BigDecimal::add);
        }
        needed.forEach((key, kg) -> {
            BigDecimal have = available.getOrDefault(key, Kilograms.ZERO);
            if (have.compareTo(kg) < 0) {
                throw new InsufficientCentralStockException(key, have);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CentralStockOverview getCentralStock(Actor actor) {
        List<CentralStockRow> rows = stock.findStock();
        BigDecimal total = rows.stream().map(CentralStockRow::stockKg).reduce(Kilograms.ZERO, BigDecimal::add);
        return new CentralStockOverview(rows, total, stock.findProducedFabricKg(), stock.findYarnFlow(), stock.findRecent(RECENT_LIMIT));
    }
}
