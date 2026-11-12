package com.yarnshipment.shipment.domain.model;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın tarafları; ad ve plaka oluşturma anındaki anlık görüntüdür.
 *
 * @param subcontractorId fasoncu
 * @param subcontractorTitle fasoncu unvanı
 * @param driverId atanmış şoför
 * @param driverName şoför adı
 * @param plateNo araç plakası
 * @param sourceSubcontractorId iplik başka bir fasoncudan gidiyorsa gönderen fasoncu; boşsa merkezden
 * @param sourceSubcontractorTitle gönderen fasoncu unvanı
 * @param directTransferSupplier doluysa iplik depoya girmeden kapıdan aktarılır; gelen tedarikçi
 * @param directTransferWaybillNo kapıdan aktarılan ipliğin geliş irsaliye numarası
 */
public record ShipmentParties(
        UUID subcontractorId,
        String subcontractorTitle,
        @Nullable UUID driverId,
        @Nullable String driverName,
        @Nullable String plateNo,
        @Nullable UUID sourceSubcontractorId,
        @Nullable String sourceSubcontractorTitle,
        @Nullable String directTransferSupplier,
        @Nullable String directTransferWaybillNo) {

    public ShipmentParties {
        Objects.requireNonNull(subcontractorId, "subcontractorId");
        Objects.requireNonNull(subcontractorTitle, "subcontractorTitle");
        if ((sourceSubcontractorId == null) != (sourceSubcontractorTitle == null)) {
            throw new InvalidNewShipmentException("sourceSubcontractorId", "source id and title come together");
        }
        if (subcontractorId.equals(sourceSubcontractorId)) {
            throw new InvalidNewShipmentException("sourceSubcontractorId", "source and destination must differ");
        }
        if (directTransferSupplier != null && directTransferSupplier.isBlank()) {
            throw new InvalidNewShipmentException("directTransferSupplier", "supplier must not be blank");
        }
        if (directTransferSupplier != null && sourceSubcontractorId != null) {
            throw new InvalidNewShipmentException(
                    "directTransferSupplier", "direct transfer comes from the door, not from a subcontractor");
        }
        if (directTransferSupplier == null && directTransferWaybillNo != null) {
            throw new InvalidNewShipmentException("directTransferWaybillNo", "waybill needs a supplier");
        }
    }

    /** Fasoncudan fasoncuya sevkiyat; kapıdan aktarma yok. */
    public ShipmentParties(
            UUID subcontractorId,
            String subcontractorTitle,
            @Nullable UUID driverId,
            @Nullable String driverName,
            @Nullable String plateNo,
            @Nullable UUID sourceSubcontractorId,
            @Nullable String sourceSubcontractorTitle) {
        this(
                subcontractorId,
                subcontractorTitle,
                driverId,
                driverName,
                plateNo,
                sourceSubcontractorId,
                sourceSubcontractorTitle,
                null,
                null);
    }

    /** İplik depoya girmeden kapıdan aktarılıyor mu. */
    public boolean directTransfer() {
        return directTransferSupplier != null;
    }

    /** Merkezden çıkan sevkiyat: gönderen fasoncu yok. */
    public ShipmentParties(
            UUID subcontractorId,
            String subcontractorTitle,
            @Nullable UUID driverId,
            @Nullable String driverName,
            @Nullable String plateNo) {
        this(subcontractorId, subcontractorTitle, driverId, driverName, plateNo, null, null);
    }

    /** SEC-13: şoför adı kişisel veridir. */
    @Override
    public String toString() {
        return "ShipmentParties[subcontractorId=" + subcontractorId + ", driverId=" + driverId + ", driverName=***]";
    }
}
