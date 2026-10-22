package com.yarnshipment.bag.domain.model;

/** Çuvalın yaşam döngüsü; veritabanındaki {@code bag.status} CHECK kısıtıyla aynıdır. */
public enum BagStatus {
    CREATED,
    RESERVED,
    SHIPPED,
    DELIVERED,
    VOID
}
