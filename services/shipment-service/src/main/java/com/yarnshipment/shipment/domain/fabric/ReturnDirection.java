package com.yarnshipment.shipment.domain.fabric;

/** İplik hareketinin yönü (analiz 5.9). */
public enum ReturnDirection {
    /** Fasoncunun kullanmadığı iplik depoya döner; bakiyeden ve fire tabanından düşer. Yeni kayıtlar yalnızca budur. */
    RETURN,
    /**
     * Artık yeni kayıt açılmaz (depo fasoncunun stoğundan iplik girmez); yalnızca eski kayıtlar okunabilsin ve
     * eklemeli (append-only) hareket geçmişi bozulmasın diye durur. Bakiyeden düşer, fire tabanını etkilemez.
     */
    FROM_SUBCONTRACTOR
}
