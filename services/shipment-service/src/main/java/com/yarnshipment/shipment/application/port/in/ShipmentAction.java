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
    ASSIGN_BAGS(Permission.SHIPMENT_CREATE, EnumSet.of(ShipmentStatus.DRAFT)),
    RECORD_DISPATCH(Permission.DISPATCH_RECORD, EnumSet.of(ShipmentStatus.DRAFT)),
    CANCEL(Permission.SHIPMENT_CANCEL, EnumSet.of(ShipmentStatus.DRAFT, ShipmentStatus.LOADED)),
    CONFIRM_PICKUP(Permission.DRIVER_DECLARATION_RECORD, EnumSet.of(ShipmentStatus.LOADED)),
    DECLARE_ARRIVAL(Permission.DRIVER_DECLARATION_RECORD, EnumSet.of(ShipmentStatus.IN_TRANSIT)),
    RECORD_DELIVERY(Permission.DELIVERY_RECORD, EnumSet.of(ShipmentStatus.AWAITING_DELIVERY)),
    /** Teslimden sonraki 3 iş günü sınırı discrepancy-service'te kontrol edilir. */
    OPEN_QUALITY_CLAIM(
            Permission.QUALITY_CLAIM_OPEN,
            EnumSet.of(ShipmentStatus.CHECKING, ShipmentStatus.IN_REVIEW, ShipmentStatus.COMPLETED)),
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
