package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import java.util.List;

/**
 * Sayfalı uyuşmazlık listesi (API-06).
 *
 * @param items bu sayfadaki uyuşmazlıklar
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 */
public record DiscrepancyPage(List<Discrepancy> items, int number, int size, long totalElements) {

    public DiscrepancyPage {
        items = List.copyOf(items);
    }

    public static DiscrepancyPage empty(int number, int size) {
        return new DiscrepancyPage(List.of(), number, size, 0);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
