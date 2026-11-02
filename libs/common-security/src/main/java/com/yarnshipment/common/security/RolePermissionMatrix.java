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
        matrix.put(Role.DRIVER, permissionsForDriver());
        matrix.put(Role.SUBCONTRACTOR, permissionsForSubcontractor());
        matrix.put(Role.QUALITY, permissionsForQuality());
        matrix.put(Role.MANAGER, permissionsForManager());
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
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_VIEW,
                Permission.EVIDENCE_INVALIDATE,
                Permission.CORRECTION_CREATE,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW,
                Permission.DISCREPANCY_RESOLVE,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW);
    }

    /** Teslim verisi kör girişe tabidir; DELIVERY_VIEW karşılaştırma bitene kadar serviste kısıtlanır. */
    private static Set<Permission> permissionsForWarehouse() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.BAG_CREATE,
                Permission.SHIPMENT_VIEW,
                Permission.SHIPMENT_CREATE,
                Permission.DISPATCH_RECORD,
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_UPLOAD_DISPATCH,
                Permission.EVIDENCE_VIEW);
    }

    /** Karar Y1: şoför çıkış kilosunu görmez; DISPATCH_VIEW yalnızca çuval sayısı ve mühür no içindir. */
    private static Set<Permission> permissionsForDriver() {
        return EnumSet.of(
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_RECORD,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.EVIDENCE_UPLOAD_DRIVER,
                Permission.EVIDENCE_VIEW);
    }

    /** Çıkış verisi kör girişe tabidir; DISPATCH_VIEW karşılaştırma bitene kadar serviste kısıtlanır. */
    private static Set<Permission> permissionsForSubcontractor() {
        return EnumSet.of(
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.DELIVERY_RECORD,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_UPLOAD_DELIVERY,
                Permission.EVIDENCE_UPLOAD_CLAIM,
                Permission.EVIDENCE_VIEW,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW);
    }

    private static Set<Permission> permissionsForQuality() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.LOT_QUALITY_LOCK,
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_UPLOAD_CLAIM,
                Permission.EVIDENCE_VIEW,
                Permission.QUALITY_CLAIM_OPEN,
                Permission.DISCREPANCY_VIEW,
                Permission.DISCREPANCY_RESOLVE,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW);
    }

    private static Set<Permission> permissionsForManager() {
        return EnumSet.of(
                Permission.MASTER_DATA_VIEW,
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_VIEW,
                Permission.DISCREPANCY_VIEW,
                Permission.AUDIT_VIEW,
                Permission.REPORT_VIEW);
    }

    private static Set<Permission> permissionsForSystem() {
        return EnumSet.of(
                Permission.SHIPMENT_VIEW,
                Permission.DISPATCH_VIEW,
                Permission.DRIVER_DECLARATION_VIEW,
                Permission.DELIVERY_VIEW,
                Permission.EVIDENCE_VIEW,
                Permission.COMPARISON_RUN);
    }
}
