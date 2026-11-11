package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.BalanceSummary;
import com.yarnshipment.shipment.application.port.in.balance.GetBalancesQuery;
import com.yarnshipment.shipment.application.port.in.balance.GetBalancesUseCase;
import com.yarnshipment.shipment.application.port.in.balance.ListBalanceMovementsUseCase;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import com.yarnshipment.shipment.application.port.out.ledger.BalanceQueryPort;
import com.yarnshipment.shipment.domain.balance.Kilograms;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fasoncu bakiyesini ve hareket defterini okur; bakiye her zaman hareketlerin toplamıdır (KR-13). */
@Service
public class BalanceQueryService implements GetBalancesUseCase, ListBalanceMovementsUseCase {

    private final BalanceQueryPort balances;

    public BalanceQueryService(BalanceQueryPort balances) {
        this.balances = balances;
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceSummary getBalances(GetBalancesQuery query) {
        List<BalanceRow> rows = balances.findBalances(query.subcontractorId());
        BigDecimal total = rows.stream().map(BalanceRow::balanceKg).reduce(Kilograms.ZERO, BigDecimal::add);
        return new BalanceSummary(rows, total);
    }

    @Override
    @Transactional(readOnly = true)
    public MovementPage listBalanceMovements(ListMovementsQuery query) {
        return balances.findMovements(query);
    }
}
