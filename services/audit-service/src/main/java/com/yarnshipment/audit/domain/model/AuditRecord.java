package com.yarnshipment.audit.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Zincire yazılmış değiştirilemez bir denetim kaydı. Kişisel veri sızdırmamak için IP, cihaz ve user-agent okuma
 * modelinde yer almaz.
 *
 * @param id zincirdeki sıra numarası
 * @param occurredAt olayın gerçekleştiği zaman
 * @param recordedAt kaydın zincire eklendiği zaman
 * @param origin olayın kaynağı
 * @param actor işlemi yapan; kullanıcısız işlerde servis hesabı
 * @param action olay türü veya yetki kararı için {@code AUTHORIZATION}
 * @param decision yalnızca yetki kararı kayıtlarında {@code ALLOWED} ya da {@code DENIED}
 * @param entity etkilenen kayıt
 * @param before işlemden önceki değer
 * @param after işlemden sonraki değer; olay kaydında olayın gövdesi
 */
public record AuditRecord(
        long id,
        Instant occurredAt,
        Instant recordedAt,
        Origin origin,
        Actor actor,
        String action,
        @Nullable String decision,
        @Nullable Entity entity,
        @Nullable Map<String, Object> before,
        @Nullable Map<String, Object> after) {

    /**
     * İşlemi yapan.
     *
     * @param userId Keycloak kullanıcı kimliği
     * @param role kullanıcının rolü ya da rolleri
     * @param clientId kullanıcısız işlerde çağıran servisin client kimliği
     */
    public record Actor(
            @Nullable UUID userId,
            @Nullable String role,
            @Nullable String clientId) {}

    /**
     * Etkilenen kayıt.
     *
     * @param type kayıt türü, ör. {@code Shipment}
     * @param id kayıt kimliği
     */
    public record Entity(String type, @Nullable UUID id) {}

    /**
     * Olayın kaynağı.
     *
     * @param sourceService olayı üreten servis
     * @param eventId CloudEvents {@code id}
     * @param traceId izleme kimliği
     */
    public record Origin(
            String sourceService, UUID eventId, @Nullable String traceId) {}
}
