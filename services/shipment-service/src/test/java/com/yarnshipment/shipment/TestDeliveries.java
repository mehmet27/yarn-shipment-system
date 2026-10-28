package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.model.DeliveryDocument;
import com.yarnshipment.shipment.domain.model.DeliveryMeasurement;
import com.yarnshipment.shipment.domain.model.ScanMethod;
import com.yarnshipment.shipment.domain.model.SealCondition;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Testlerde kullanılan teslim girişleri. */
public final class TestDeliveries {

    public static final String GROSS_KG = "1995.250";
    public static final String TARE_KG = "18.000";

    private TestDeliveries() {}

    /** {@link #measurement()} gibi, ama imzalı irsaliye fotoğrafı eklenmiş. */
    public static DeliveryMeasurement measurementWithSignedDocument(UUID evidenceId) {
        DeliveryMeasurement base = measurement();
        return new DeliveryMeasurement(
                base.grossKg(),
                base.tareKg(),
                base.bagCount(),
                base.sealCondition(),
                base.sealNo(),
                base.scannedKeys(),
                base.scanMethod(),
                new DeliveryDocument(null, null, null, null, evidenceId));
    }

    /** Mührü sağlam, iki çuvalı okutulmuş giriş; biri sevkiyatta yok. */
    public static DeliveryMeasurement measurement() {
        return new DeliveryMeasurement(
                new BigDecimal(GROSS_KG),
                new BigDecimal(TARE_KG),
                TestShipments.BAG_COUNT,
                SealCondition.INTACT,
                "M-0001",
                Set.of("UK-0001", "UK-9999"),
                ScanMethod.SCAN,
                DeliveryDocument.NONE);
    }
}
