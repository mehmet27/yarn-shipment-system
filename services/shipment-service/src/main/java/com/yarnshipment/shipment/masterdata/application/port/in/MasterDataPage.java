package com.yarnshipment.shipment.masterdata.application.port.in;

import java.util.List;

/**
 * Ana veri sayfası.
 *
 * @param items bu sayfadaki kayıtlar
 * @param number sayfa numarası
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 * @param <T> kayıt tipi
 */
public record MasterDataPage<T>(List<T> items, int number, int size, long totalElements) {

    public MasterDataPage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
