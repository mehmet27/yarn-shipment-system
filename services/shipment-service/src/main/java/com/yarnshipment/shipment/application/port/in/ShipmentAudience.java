package com.yarnshipment.shipment.application.port.in;

/** Sevkiyat verisinin hangi görünümle döneceği (API-09, SEC-05). */
public enum ShipmentAudience {
    /** Admin, kalite, müdür, sistem. */
    INTERNAL,
    /** Depo. */
    WAREHOUSE,
    /** Fasoncu: yalnızca kendi sevkiyatları; beklenen yük yok. */
    SUBCONTRACTOR
}
