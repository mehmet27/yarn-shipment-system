package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.YarnFlow;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockEntry;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.ReceiptDetails;
import com.yarnshipment.shipment.domain.stock.SupplierReceipt;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Merkez iplik stoku defteri. */
public interface CentralStockPort {

    /** Tedarikçisiz iplik girişini (sistem girişi) yazar. */
    default void saveReceipt(CentralStockMovement receipt) {
        saveReceipt(receipt, null);
    }

    /** Merkeze iplik girişini ve tedarikçi bilgisini yazar. */
    void saveReceipt(CentralStockMovement receipt, @Nullable ReceiptDetails details);

    /** Üretimi ve tükettiği iplikleri yazar. */
    void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions);

    /**
     * Stoktan düşen işlemleri transaction sonuna kadar sıraya sokar (CONC-07). "Stok yetiyor mu" denetimi ile düşüş
     * arasına başka bir düşüş giremez; aynı anda gelen iki işlem stoğu birlikte eksiye düşüremez. Denetimden önce
     * çağrılır.
     */
    void lockForDeduction();

    List<CentralStockRow> findStock();

    /**
     * Sevkiyatın merkez stoktan düştüğü iplik tanımlarından, düşüşten sonra stoğu eksi kalanlar; satırdaki kilo
     * düşüşten sonraki (eksi) stoktur. Boş liste: stok yetiyor.
     */
    List<CentralStockRow> findShortagesAfterDispatch(UUID shipmentId);

    /**
     * Sevkiyatın merkez stoktan düştüğü, Tanımlar'daki hiçbir iplikle eşleşmeyen (serbest metin) iplik adları. Reçetesi
     * olmayan eski sevkiyatın "(reçetesiz)" satırı sayılmaz. Boş liste: bütün iplikler tanımlı.
     */
    List<String> findUnlistedYarnsOfDispatch(UUID shipmentId);

    /** Verilen iplik tanımlarından <b>aktif</b> olanların adları; pasif ya da olmayan tanım sonuçta yer almaz. */
    Map<UUID, String> findActiveProductNames(Collection<UUID> productIds);

    /** Gelen ipliğin merkezde / fasoncularda / üretimde dağılımı. */
    YarnFlow findYarnFlow();

    List<CentralStockEntry> findRecent(int limit);

    /** Aktif iplik tedarikçisinin adı; tanım yoksa ya da pasifse boş. */
    Optional<String> findActiveSupplierName(UUID supplierId);

    /** Tedarikçi ve iplik cinsi başına bugüne kadar gelen iplik. */
    List<SupplierReceipt> findSupplierReceipts();

    /** Merkez fabrikada bugüne kadar üretilen toplam kumaş (kg). */
    BigDecimal findProducedFabricKg();
}
