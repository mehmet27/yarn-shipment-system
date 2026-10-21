package com.yarnshipment.common.security;

import org.springframework.security.core.GrantedAuthority;

/**
 * Endpoint'lerin istediği ince taneli izinler. Spring Security'de yetki adı izin adının kendisidir:
 * {@code @PreAuthorize("hasAuthority('DISPATCH_RECORD')")}. Veriye bağlı kurallar (kendi sevkiyatı, kör giriş,
 * durum) burada değil, kapsam kontrolünde ve ilgili serviste uygulanır.
 */
public enum Permission implements GrantedAuthority {
    MASTER_DATA_VIEW(AccessType.READ),
    MASTER_DATA_MANAGE(AccessType.WRITE),
    USER_MANAGE(AccessType.WRITE),
    SETTINGS_MANAGE(AccessType.WRITE),
    LOT_QUALITY_LOCK(AccessType.WRITE),

    SHIPMENT_VIEW(AccessType.READ),
    SHIPMENT_CREATE(AccessType.WRITE),
    SHIPMENT_CANCEL(AccessType.WRITE),

    DISPATCH_RECORD(AccessType.WRITE),
    DISPATCH_VIEW(AccessType.READ),

    DRIVER_DECLARATION_RECORD(AccessType.WRITE),
    DRIVER_DECLARATION_VIEW(AccessType.READ),

    DELIVERY_RECORD(AccessType.WRITE),
    DELIVERY_VIEW(AccessType.READ),

    EVIDENCE_UPLOAD_DISPATCH(AccessType.WRITE),
    EVIDENCE_UPLOAD_DRIVER(AccessType.WRITE),
    EVIDENCE_UPLOAD_DELIVERY(AccessType.WRITE),
    EVIDENCE_UPLOAD_CLAIM(AccessType.WRITE),
    EVIDENCE_VIEW(AccessType.READ),
    EVIDENCE_INVALIDATE(AccessType.WRITE),

    CORRECTION_CREATE(AccessType.WRITE),
    QUALITY_CLAIM_OPEN(AccessType.WRITE),
    DISCREPANCY_VIEW(AccessType.READ),
    DISCREPANCY_RESOLVE(AccessType.WRITE),

    AUDIT_VIEW(AccessType.READ),
    REPORT_VIEW(AccessType.READ),

    /** Yalnızca SYSTEM: olaydan tetiklenen karşılaştırma. */
    COMPARISON_RUN(AccessType.WRITE);

    private final AccessType accessType;

    Permission(AccessType accessType) {
        this.accessType = accessType;
    }

    /** Veri oluşturan veya değiştiren izin mi? MANAGER rolünün hiçbir yazma izni olmamalı. */
    public boolean isWrite() {
        return accessType == AccessType.WRITE;
    }

    @Override
    public String getAuthority() {
        return name();
    }

    private enum AccessType {
        READ,
        WRITE
    }
}
