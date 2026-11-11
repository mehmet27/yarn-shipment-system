package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.model.DispatchMeasurement;
import com.yarnshipment.shipment.domain.model.Weighing.TareBasis;
import java.math.BigDecimal;

/** Testlerde kullanılan çıkış tartımı girdileri. */
public final class TestDispatches {

    public static final int BAG_COUNT = 40;

    private TestDispatches() {}

    public static DispatchMeasurement measurement() {
        return new DispatchMeasurement(new BigDecimal("2000.500"), new BigDecimal("18.000"), BAG_COUNT);
    }

    /** Çuval başı 0,5 kg standart dara, %5 sınır: 40 çuval için 20 kg beklenir. */
    public static TareBasis tareBasis(int bagCount) {
        return new TareBasis(new BigDecimal("0.500").multiply(BigDecimal.valueOf(bagCount)), new BigDecimal("5.00"));
    }
}
