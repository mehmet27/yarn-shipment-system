package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.fabric.YarnStockRow;
import java.util.List;
import java.util.UUID;

/** Fasoncunun açık sevkiyatlarında kumaşa girebilecek iplik. */
public interface YarnStockPort {

    /**
     * Fire dosyası kapanmamış sevkiyatların kalan ipliği (sevkiyatta gelen eksi kumaşa girmiş, dönmüş, faturalanmış ya
     * da itirazla düşmüş kilo), iplik ve partiye göre.
     */
    List<YarnStockRow> findOpenStock(UUID subcontractorId);
}
