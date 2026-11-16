package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.YarnFlow;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Merkez iplik stoku defteri. */
public interface CentralStockPort {

    /** Merkeze iplik girişini yazar. */
    void saveReceipt(CentralStockMovement receipt);

    /** Üretimi ve tükettiği iplikleri yazar. */
    void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions);

    List<CentralStockRow> findStock();

    /**
     * Sevkiyatın merkez stoktan düştüğü iplik tanımlarından, düşüşten sonra stoğu eksi kalanlar; satırdaki kilo
     * düşüşten sonraki (eksi) stoktur. Boş liste: stok yetiyor.
     */
    List<CentralStockRow> findShortagesAfterDispatch(UUID shipmentId);

    /** Verilen iplik tanımlarından <b>aktif</b> olanların adları; pasif ya da olmayan tanım sonuçta yer almaz. */
    Map<UUID, String> findActiveProductNames(Collection<UUID> productIds);

    /** Gelen ipliğin merkezde / fasoncularda / üretimde dağılımı. */
    YarnFlow findYarnFlow();

    List<CentralStockMovement> findRecent(int limit);

    /** Merkez fabrikada bugüne kadar üretilen toplam kumaş (kg). */
    BigDecimal findProducedFabricKg();
}
