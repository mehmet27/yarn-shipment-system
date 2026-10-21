package com.yarnshipment.shipment.application.port.in;

/** Sevkiyat verisinin hangi görünümle döneceği (API-09, SEC-05). */
public enum ShipmentAudience {
    /** Admin, kalite, yönetici, sistem: iki tarafın verisi. */
    INTERNAL,
    /** Depo: teslim tarafı yok. */
    WAREHOUSE,
    /** Fasoncu: çıkış tarafı ve beklenen yük yok. */
    SUBCONTRACTOR,
    /** Şoför: kilo yok, yalnızca çuval sayısı ve mühür numarası. */
    DRIVER
}
