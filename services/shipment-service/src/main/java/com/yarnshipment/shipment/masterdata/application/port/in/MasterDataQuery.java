package com.yarnshipment.shipment.masterdata.application.port.in;

import org.jspecify.annotations.Nullable;

/**
 * Ana veri liste isteği.
 *
 * @param text arama metni; ad, unvan veya plakada büyük-küçük harf duyarsız aranır
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu, 1-100 (API-06)
 * @param includeInactive pasife alınmış kayıtlar da gelsin (yalnızca yönetici)
 */
public record MasterDataQuery(@Nullable String text, int page, int size, boolean includeInactive) {

    public static final int MAX_PAGE_SIZE = 100;

    /** Yalnızca etkin kayıtlar. */
    public MasterDataQuery(@Nullable String text, int page, int size) {
        this(text, page, size, false);
    }

    public MasterDataQuery {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid page request page=" + page + " size=" + size);
        }
    }
}
