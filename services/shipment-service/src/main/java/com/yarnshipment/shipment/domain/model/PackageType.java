package com.yarnshipment.shipment.domain.model;

/** Sevkiyattaki ambalaj birimi; bag-service'teki türle aynı değerleri taşır. Standart dara türe göre seçilir. */
public enum PackageType {
    BAG,
    BOX,
    PALLET,
    BOBBIN
}
