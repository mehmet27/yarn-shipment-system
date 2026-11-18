package com.yarnshipment.shipment.domain.stock;

import java.io.Serial;

/** Girişte yazılan parti kullanılamıyor (409). {@code code} ekrandaki mesajı seçer. */
public final class YarnLotConflictException extends RuntimeException {

    /** Parti ilk kez yazılıyor; kalite sınıfı seçilmeli. */
    public static final String GRADE_REQUIRED = "YARN_LOT_GRADE_REQUIRED";
    /** Parti numarası başka bir iplik cinsine ait. */
    public static final String OTHER_YARN = "YARN_LOT_OTHER_YARN";
    /** Parti numarası başka bir tedarikçiye ait. */
    public static final String OTHER_SUPPLIER = "YARN_LOT_OTHER_SUPPLIER";
    /** Kullanıcının parti açabileceği lokasyon yok. */
    public static final String NO_SITE = "YARN_LOT_NO_SITE";

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;

    public YarnLotConflictException(String code) {
        super("Yarn lot conflict code=" + code);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
