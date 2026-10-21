package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import java.math.BigDecimal;

/** Testlerde kullanılan çıkış tartımı girdileri. */
public final class TestDispatches {

    public static final int BAG_COUNT = 40;
    public static final String SEAL_NO = "M-0001";

    private TestDispatches() {}

    public static DispatchMeasurement measurement() {
        return new DispatchMeasurement(
                new BigDecimal("2000.500"), new BigDecimal("18.000"), BAG_COUNT, "KF-1", SEAL_NO);
    }
}
