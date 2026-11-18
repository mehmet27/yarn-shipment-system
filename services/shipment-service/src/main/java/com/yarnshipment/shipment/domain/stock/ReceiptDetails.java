package com.yarnshipment.shipment.domain.stock;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Merkeze iplik girişinin tedarikçi bilgisi; hareketle birlikte saklanır.
 *
 * @param supplierId tedarikçi tanımı; kapıdan aktarmada tanımla eşleşmeyen tedarikçide boş
 * @param supplierName tedarikçinin hareket anındaki adı
 * @param lotId girişte yazılan parti, isteğe bağlı; sevkiyat ve iplik dönüşündeki parti seçimi bu partidir
 * @param lotNo partinin numarası
 * @param waybillNo tedarikçi irsaliye numarası, isteğe bağlı
 */
public record ReceiptDetails(
        @Nullable UUID supplierId,
        String supplierName,
        @Nullable UUID lotId,
        @Nullable String lotNo,
        @Nullable String waybillNo) {

    public static final int MAX_WAYBILL_LENGTH = 128;

    public ReceiptDetails {
        Objects.requireNonNull(supplierName, "supplierName");
        supplierName = supplierName.strip();
        waybillNo = waybillNo == null || waybillNo.isBlank() ? null : waybillNo.strip();
        if (supplierName.isEmpty()) {
            throw new InvalidLedgerInputException("supplierId");
        }
        if (waybillNo != null && waybillNo.length() > MAX_WAYBILL_LENGTH) {
            throw new InvalidLedgerInputException("waybillNo");
        }
    }
}
