package com.yarnshipment.shipment.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.DeliveryDocument;
import com.yarnshipment.shipment.adapter.in.web.api.model.DeliveryReceiptRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.ScanMethod;
import com.yarnshipment.shipment.domain.model.DeliveryMeasurement;
import com.yarnshipment.shipment.domain.model.SealCondition;
import java.math.BigDecimal;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Teslim isteğini domain değerlerine çevirir. Kilo dizeleri sözleşmedeki desenle zaten doğrulanmıştır. */
final class DeliveryRequests {

    private DeliveryRequests() {}

    static DeliveryMeasurement toMeasurement(DeliveryReceiptRequest request) {
        ScanMethod scanMethod = request.getScanMethod();
        return new DeliveryMeasurement(
                new BigDecimal(request.getGrossKg()),
                new BigDecimal(request.getTareKg()),
                request.getBagCount(),
                SealCondition.valueOf(request.getSealCondition().name()),
                request.getSealNo(),
                Set.copyOf(request.getBagUniqueKeys()),
                scanMethod == null
                        ? com.yarnshipment.shipment.domain.model.ScanMethod.SCAN
                        : com.yarnshipment.shipment.domain.model.ScanMethod.valueOf(scanMethod.name()),
                toDocument(request.getDocument()));
    }

    private static com.yarnshipment.shipment.domain.model.DeliveryDocument toDocument(
            @Nullable DeliveryDocument document) {
        if (document == null) {
            return com.yarnshipment.shipment.domain.model.DeliveryDocument.NONE;
        }
        String netKg = document.getNetKg();
        return new com.yarnshipment.shipment.domain.model.DeliveryDocument(
                document.getShipmentNo(),
                document.getDate(),
                netKg == null ? null : new BigDecimal(netKg),
                document.getBagCount(),
                document.getSignedDocumentEvidenceId());
    }
}
