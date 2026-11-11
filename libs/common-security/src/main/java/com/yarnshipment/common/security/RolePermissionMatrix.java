package com.yarnshipment.common.security;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ön analiz Bölüm 6.2 yetki matrisinin koddaki tek kaynağı (yetkilendirme analizi, "Rol ve izin modeli").
 * Matris değişirse yalnızca bu sınıf değişir; endpoint'ler rol değil izin ister.
 */
public final class RolePermissionMatrix {

    private static final Map<Role, Set<Permission>> PERMISSIONS_BY_ROLE = createMatrix();

    private RolePermissionMatrix() {}

    /** Rollerin izinlerinin birleşimi. */
    public static Set<Permission> permissionsOf(Collection<Role> roles) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        roles.forEach(role -> permissions.addAll(PERMISSIONS_BY_ROLE.getOrDefault(role, Set.of())));
        return Set.copyOf(permissions);
    }

    private static Map<Role, Set<Permission>> createMatrix() {
        Map<Role, Set<Permission>> matrix = new EnumMap<>(Role.class);
        matrix.put(Role.ADMIN, permissionsForAdmin());
        matrix.put(Role.WAREHOUSE, permissionsForWarehouse());
        matrix.put(Role.QUALITY, permissionsForQuality());
        matrix.put(Role.MANAGER, permissionsForManager());
        matrix.put(Role.GENERAL_MANAGER, EnumSet.of(Permission.REPORT_VIEW, Permission.BALANCE_VIEW));
        matrix.put(Role.SYSTEM, permissionsForSystem());
        return Map.copyOf(matrix);
    }

    private static Set<Permission> permissionsForAdmin() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.MASTER_DATA_MANAGE,
                Permission.USER_MANAGE,
                Permission.SETTINGS_MANAGE,
                Permission.LOT_QUALITY_LOCK,
                Permission.BAG_CREATE,
                Permission.SHIPMENT_VIEW,
                Permission.SHIPMENT_CREATE,
                Permission.SHIPMENT_CANCEL,
                Permission.SHIPMENT_COMPLETE,
                Permission.DISPATCH_VIEW,
                Permission.EVIDENCE_UPLOAD_CLAIM,
                Permission.EVIDENCE_VIEW,
                Permission.EVIDENCE_INVALIDATE,
                Permission.CORRECTION_CREATE,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW,
                Permission.DISCREPANCY_RESOLVE,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW,
                Permission.BALANCE_VIEW,
                Permission.CENTRAL_STOCK_VIEW,
                Permission.CENTRAL_STOCK_RECORD,
                Permission.CENTRAL_PRODUCTION_RECORD);
    }

    private static Set<Permission> permissionsForWarehouse() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.BAG_CREATE,
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_RECORD,
                Permission.DISPATCH_VIEW,
                Permission.EVIDENCE_UPLOAD_DISPATCH,
                Permission.EVIDENCE_VIEW,
                Permission.YARN_RETURN_RECORD,
                Permission.CENTRAL_STOCK_VIEW,
                Permission.CENTRAL_STOCK_RECORD);
    }

    private static Set<Permission> permissionsForQuality() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.LOT_QUALITY_LOCK,
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.EVIDENCE_UPLOAD_CLAIM,
                Permission.EVIDENCE_VIEW,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW,
                Permission.DISCREPANCY_RESOLVE,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW,
                Permission.FABRIC_RECEIPT_RECORD,
                Permission.BALANCE_VIEW,
                Permission.CENTRAL_STOCK_VIEW,
                Permission.CENTRAL_PRODUCTION_RECORD);
    }

    /** Müdür sevkiyatı açar, iptal eder ve irsaliye gelince tamamlar (saha analizi, 10.11.2026). */
    private static Set<Permission> permissionsForManager() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.SHIPMENT_VIEW,
                Permission.SHIPMENT_CREATE,
                Permission.SHIPMENT_CANCEL,
                Permission.SHIPMENT_COMPLETE,
                Permission.DISPATCH_VIEW,
                Permission.EVIDENCE_UPLOAD_CLAIM,
                Permission.EVIDENCE_VIEW,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW,
                Permission.FABRIC_RECEIPT_RECORD,
                Permission.YARN_RETURN_RECORD,
                Permission.FIRE_RATE_EDIT,
                Permission.INVOICE_MARK,
                Permission.BALANCE_VIEW,
                Permission.WEIGHT_MISMATCH_RESOLVE,
                Permission.CENTRAL_STOCK_VIEW);
    }

    private static Set<Permission> permissionsForSystem() {
        return EnumSet.of(Permission.SHIPMENT_VIEW, Permission.DISPATCH_VIEW, Permission.EVIDENCE_VIEW);
    }
}
