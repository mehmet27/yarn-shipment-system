package com.yarnshipment.bag.domain.model;

/** Ambalaj türü; veritabanındaki {@code bag.package_type} CHECK kısıtıyla aynıdır. */
public enum PackageType {
    BAG,
    BOX,
    PALLET,
    BOBBIN
}
