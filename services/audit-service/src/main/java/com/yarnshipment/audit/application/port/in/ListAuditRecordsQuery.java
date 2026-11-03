package com.yarnshipment.audit.application.port.in;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Denetim kaydı listesi sorgusu.
 *
 * @param filter süzgeç
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 */
public record ListAuditRecordsQuery(Filter filter, int page, int size) {

    /**
     * Süzgeç; boş alan o koşulu uygulamaz.
     *
     * @param entityType etkilenen kayıt türü
     * @param entityId etkilenen kayıt
     * @param actorUserId işlemi yapan kullanıcı
     * @param action olay türü
     * @param sourceService olayı üreten servis
     * @param from bu zamandan itibaren (dahil)
     * @param to bu zamana kadar (hariç)
     */
    public record Filter(
            @Nullable String entityType,
            @Nullable UUID entityId,
            @Nullable UUID actorUserId,
            @Nullable String action,
            @Nullable String sourceService,
            @Nullable Instant from,
            @Nullable Instant to) {}
}
