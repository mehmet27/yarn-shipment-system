package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.balance.BalanceRow;
import com.yarnshipment.shipment.application.port.in.balance.ListMovementsQuery;
import com.yarnshipment.shipment.application.port.in.balance.MovementPage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bakiye defterini okuma portu; persistence adapter'ı uygular (PERS-08). */
public interface BalanceQueryPort {

    /** Fasoncu ve iplik cinsi başına bakiye ve yoldaki kilo; fasoncu filtresi boşsa hepsi. */
    List<BalanceRow> findBalances(@Nullable UUID subcontractorId);

    MovementPage findMovements(ListMovementsQuery query);

    /** Sevkiyatın DISPATCH hareketlerinin iplik cinsine göre toplamı; kumaş dağıtımının oran kaynağı. */
    Map<String, BigDecimal> findDispatchedByYarn(UUID shipmentId);
}
