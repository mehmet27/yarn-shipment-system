package com.yarnshipment.shipment.application.port.in;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.EnumSet;
import java.util.Set;

/**
 * Kullanıcının bir sevkiyatta yapabileceği işlemler (yetkilendirme analizi, "Duruma bağlı yazma ve kilit"). Her işlem
 * bir izin ve izin verildiği durumlarla tanımlıdır; frontend butonu yalnızca bu listeye göre gösterir (FE-SEC-03).
 */
public enum ShipmentAction {
    ASSIGN_BAGS(Permission.DISPATCH_RECORD, EnumSet.of(ShipmentStatus.DRAFT)),
    RECORD_DISPATCH(Permission.DISPATCH_RECORD, EnumSet.of(ShipmentStatus.DRAFT)),
    CANCEL(
            Permission.SHIPMENT_CANCEL,
            EnumSet.of(ShipmentStatus.DRAFT, ShipmentStatus.WEIGHT_MISMATCH, ShipmentStatus.AWAITING_DELIVERY)),
    /** Müdür kantar uyuşmazlığına karar verir (iptal, çuval ekle/çıkar, reçeteyi güncelle). */
    RESOLVE_WEIGHT_MISMATCH(Permission.SHIPMENT_CANCEL, EnumSet.of(ShipmentStatus.WEIGHT_MISMATCH)),
    /** İrsaliye müdüre ulaştı. */
    COMPLETE(Permission.SHIPMENT_COMPLETE, EnumSet.of(ShipmentStatus.AWAITING_DELIVERY)),
    OPEN_QUALITY_CLAIM(Permission.QUALITY_CLAIM_OPEN, EnumSet.of(ShipmentStatus.COMPLETED)),
    CREATE_CORRECTION(Permission.CORRECTION_CREATE, EnumSet.complementOf(EnumSet.of(ShipmentStatus.CANCELLED)));

    private final Permission requiredPermission;
    // Set.copyOf değiştirilemez küme döner; Error Prone bunu tipten çıkaramaz.
    @SuppressWarnings("ImmutableEnumChecker")
    private final Set<ShipmentStatus> allowedStatuses;

    ShipmentAction(Permission requiredPermission, Set<ShipmentStatus> allowedStatuses) {
        this.requiredPermission = requiredPermission;
        this.allowedStatuses = Set.copyOf(allowedStatuses);
    }

    public Permission requiredPermission() {
        return requiredPermission;
    }

    public boolean isAllowedIn(ShipmentStatus status) {
        return allowedStatuses.contains(status);
    }
}
