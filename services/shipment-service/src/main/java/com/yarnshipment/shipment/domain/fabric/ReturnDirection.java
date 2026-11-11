package com.yarnshipment.shipment.domain.fabric;

/** İplik hareketinin yönü (analiz 5.9). */
public enum ReturnDirection {
    /** Fasoncunun kullanmadığı iplik merkeze döner; bakiyeden ve fire tabanından düşer. */
    RETURN,
    /** Fasoncu kendi stoğundan iplik gönderdi; bakiyeden düşer, fire tabanını etkilemez. */
    FROM_SUBCONTRACTOR
}
