package com.yarnshipment.shipment.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockOverview;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.GetCentralStockUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
    private final Clock clock;

    public CentralStockService(CentralStockPort stock, Clock clock) {
        this.stock = stock;
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
        stock.saveProduction(production, consumptions);
        return production;
    }

    @Override
    @Transactional(readOnly = true)
    public CentralStockOverview getCentralStock(Actor actor) {
        List<CentralStockRow> rows = stock.findStock();
        BigDecimal total = rows.stream().map(CentralStockRow::stockKg).reduce(Kilograms.ZERO, BigDecimal::add);
        return new CentralStockOverview(
                rows, total, stock.findProducedFabricKg(), stock.findRecent(RECENT_LIMIT));
    }
}
