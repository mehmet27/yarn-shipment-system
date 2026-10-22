package com.yarnshipment.shipment.masterdata.domain.model;

/** Ana veri kaydının etkinlik durumu. Pasif kayıt listelerde görünmez; kayıt silinmez. */
public enum MasterDataStatus {
    ACTIVE,
    INACTIVE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
