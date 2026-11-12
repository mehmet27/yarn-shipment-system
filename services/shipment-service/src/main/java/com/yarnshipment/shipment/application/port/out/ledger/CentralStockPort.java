package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.List;

/** Merkez iplik stoku defteri. */
public interface CentralStockPort {

    /** Merkeze iplik girişini yazar. */
    void saveReceipt(CentralStockMovement receipt);

    /** Üretimi ve tükettiği iplikleri yazar. */
    void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions);

    List<CentralStockRow> findStock();

    List<CentralStockMovement> findRecent(int limit);

    /** Merkez fabrikada bugüne kadar üretilen toplam kumaş (kg). */
    BigDecimal findProducedFabricKg();
}
