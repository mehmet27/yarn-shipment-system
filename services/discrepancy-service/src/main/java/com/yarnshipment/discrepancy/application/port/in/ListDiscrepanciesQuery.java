package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Uyuşmazlık listesi sorgusu.
 *
 * @param actor isteyen
 * @param filter süzgeç
 * @param page sayfa numarası, 0'dan başlar
 * @param size sayfa boyu
 */
public record ListDiscrepanciesQuery(Actor actor, Filter filter, int page, int size) {

    /**
     * Süzgeç; boş alan o koşulu uygulamaz.
     *
     * @param status durum
     * @param severity öncelik
     * @param assigneeId sorumlu
     * @param shipmentId sevkiyat
     * @param overdue true ise yalnızca vadesi geçmiş ve hâlâ açık (OPEN, IN_REVIEW) kayıtlar
     */
    public record Filter(
            @Nullable DiscrepancyStatus status,
            @Nullable Severity severity,
            @Nullable UUID assigneeId,
            @Nullable UUID shipmentId,
            @Nullable Boolean overdue) {

        /**
         * Aynı süzgeç, vadesi geçmiş kayıt koşuluyla.
         *
         * @param overdueOnly true ise yalnızca vadesi geçmiş açık kayıtlar
         * @return yeni süzgeç
         */
        public Filter withOverdue(@Nullable Boolean overdueOnly) {
            return new Filter(status, severity, assigneeId, shipmentId, overdueOnly);
        }
    }
}
