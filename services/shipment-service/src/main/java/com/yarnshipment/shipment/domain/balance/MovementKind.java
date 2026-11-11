package com.yarnshipment.shipment.domain.balance;

import java.math.BigDecimal;

/** Bakiye hareket türleri (analiz 5.7). */
public enum MovementKind {
    /** İrsaliyeyle tamamlanan sevkiyat; fasoncunun bakiyesini artırır. */
    DISPATCH(Sign.POSITIVE),
    /** Kumaş girişi; tüketilen iplik kadar düşer. */
    FABRIC_RECEIPT(Sign.NEGATIVE),
    /** Fasoncudan merkeze dönen iplik. */
    YARN_RETURN(Sign.NEGATIVE),
    /** Fasoncunun kendi stoğundan gönderdiği iplik. */
    YARN_FROM_SUBCONTRACTOR(Sign.NEGATIVE),
    /** Fasoncunun elindeki iplikten başka fasoncuya gönderilen kısım; gönderenin bakiyesini düşer. */
    TRANSFER_OUT(Sign.NEGATIVE),
    /** Haklı kalite itirazının karara bağlanması; itiraz kilosu iplik bakiyesinden düşer (eksi). */
    CLAIM_ADJUSTMENT(Sign.EITHER),
    /** Excel'den açılış bakiyesi. */
    OPENING(Sign.EITHER),
    /** Gerekçeli düzeltme; eski hareket silinmez, ters hareket yazılır. */
    CORRECTION(Sign.EITHER);

    private final Sign sign;

    MovementKind(Sign sign) {
        this.sign = sign;
    }

    /** Hareketin işareti bu türe uygun mu (sıfır hiçbir türde geçerli değildir). */
    public boolean accepts(BigDecimal netKg) {
        return switch (sign) {
            case POSITIVE -> netKg.signum() > 0;
            case NEGATIVE -> netKg.signum() < 0;
            case EITHER -> netKg.signum() != 0;
        };
    }

    private enum Sign {
        POSITIVE,
        NEGATIVE,
        EITHER
    }
}
