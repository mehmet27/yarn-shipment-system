package com.yarnshipment.fixture.naming;

/** NAME-16 kasıtlı ihlali. */
// İstisna: AbbreviationAsWordInName, ArchUnit kuralının test verisi, kod standartları Ek C.3
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface IShipmentPort {

    String shipmentNo();
}
