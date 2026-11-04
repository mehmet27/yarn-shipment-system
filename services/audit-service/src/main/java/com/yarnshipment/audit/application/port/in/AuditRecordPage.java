package com.yarnshipment.audit.application.port.in;

import com.yarnshipment.audit.domain.model.AuditRecord;
import java.util.List;

/**
 * Sayfalı denetim kaydı listesi (API-06).
 *
 * @param items bu sayfadaki kayıtlar, en yeni önce
 * @param number sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 * @param totalElements toplam kayıt
 */
public record AuditRecordPage(List<AuditRecord> items, int number, int size, long totalElements) {

    public AuditRecordPage {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
