package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Merkez iplik stoku defteri. */
public interface CentralStockPort {

    /** Merkeze iplik girişini yazar. */
    void saveReceipt(CentralStockMovement receipt);

    /** Üretimi ve tükettiği iplikleri yazar. */
    void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions);

    /** İplik cinslerini kilitleyip eldeki kiloları verir; stoğu olmayan cins 0 döner. */
    Map<String, BigDecimal> lockStock(Collection<String> yarnTypes);

    List<CentralStockRow> findStock();

    List<CentralStockMovement> findRecent(int limit);
}
