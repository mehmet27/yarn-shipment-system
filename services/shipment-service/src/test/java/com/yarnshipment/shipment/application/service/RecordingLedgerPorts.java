package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.application.port.out.ledger.DispatchLedgerPort;
import com.yarnshipment.shipment.application.port.out.ledger.FabricReceiptPort;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Defter ve kumaş girişi portlarının sahtesi: yazılanları tutar, okumaları testin verdiği değerlerle yanıtlar. */
final class RecordingLedgerPorts implements BalanceQueryPort, DispatchLedgerPort, FabricReceiptPort {

    private final List<UUID> dispatchedShipments = new ArrayList<>();
    private final List<FabricReceipt> receipts = new ArrayList<>();
    private final Map<String, BigDecimal> dispatchedByYarn = new LinkedHashMap<>();
    private BigDecimal sentKg = new BigDecimal("1000.000");

    void givenSentKg(String kg) {
        sentKg = new BigDecimal(kg);
    }

    void givenRecipe(String yarnType, String kg) {
        dispatchedByYarn.put(yarnType, new BigDecimal(kg));
    }

    List<UUID> dispatchedShipments() {
        return dispatchedShipments;
    }

    List<FabricReceipt> receipts() {
        return receipts;
    }

    @Override
    public BigDecimal recordDispatch(UUID shipmentId, UUID subcontractorId, Instant at, UUID createdBy) {
        dispatchedShipments.add(shipmentId);
        return sentKg;
    }

    @Override
    public List<BalanceRow> findBalances(@Nullable UUID subcontractorId) {
        return List.of();
    }

    @Override
    public MovementPage findMovements(ListMovementsQuery query) {
        return new MovementPage(List.of(), query.page(), query.size(), 0);
    }

    @Override
    public Map<String, BigDecimal> findDispatchedByYarn(UUID shipmentId) {
        return dispatchedByYarn;
    }

    @Override
    public List<FabricReceipt> findByShipment(UUID shipmentId) {
        return receipts;
    }

    @Override
    public void save(FabricReceipt receipt) {
        receipts.add(receipt);
    }
}
