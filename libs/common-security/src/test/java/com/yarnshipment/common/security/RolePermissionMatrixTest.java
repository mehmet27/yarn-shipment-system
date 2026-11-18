package com.yarnshipment.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RolePermissionMatrixTest {

    @Test
    @DisplayName("Müdürün yazma izinleri: tanımlar, partiler, sevkiyat, kayıtlar, fire, fatura, tartı, düzeltme")
    void shouldLimitManagerWrites() {
        // when
        Set<Permission> permissions = RolePermissionMatrix.permissionsOf(List.of(Role.MANAGER));

        // then
        assertThat(permissions.stream().filter(Permission::isWrite))
                .containsExactlyInAnyOrder(
                        Permission.MASTER_DATA_MANAGE,
                        Permission.LOT_QUALITY_LOCK,
                        Permission.SHIPMENT_CREATE,
                        Permission.SHIPMENT_CANCEL,
                        Permission.SHIPMENT_COMPLETE,
                        Permission.EVIDENCE_INVALIDATE,
                        Permission.CORRECTION_CREATE,
                        Permission.DISCREPANCY_DECIDE,
                        Permission.FIRE_RATE_EDIT,
                        Permission.INVOICE_MARK,
                        Permission.WEIGHT_MISMATCH_RESOLVE);
    }

    @Test
    @DisplayName("Genel müdür yalnızca rapor, tutar, bakiye, merkez stok ve ad çözmek için ana veri listesini görür")
    void shouldLimitGeneralManagerToReadOnly() {
        assertThat(RolePermissionMatrix.permissionsOf(List.of(Role.GENERAL_MANAGER)))
                .containsExactlyInAnyOrder(
                        Permission.REPORT_VIEW,
                        Permission.AMOUNT_VIEW,
                        Permission.BALANCE_VIEW,
                        Permission.CENTRAL_STOCK_VIEW,
                        Permission.MASTER_DATA_VIEW);
    }

    @Test
    @DisplayName("Tutarı (TL) yalnızca müdür ve genel müdür görür")
    void shouldLetOnlyManagersSeeAmounts() {
        assertThat(rolesHaving(Permission.AMOUNT_VIEW)).containsExactlyInAnyOrder(Role.MANAGER, Role.GENERAL_MANAGER);
    }

    @Test
    @DisplayName("Sevkiyatı yalnızca müdür açar; depo ve admin açamaz")
    void shouldNotAllowWarehouseToCreateShipments() {
        assertThat(rolesHaving(Permission.SHIPMENT_CREATE)).containsExactly(Role.MANAGER);
        assertThat(rolesHaving(Permission.BALANCE_VIEW))
                .containsExactlyInAnyOrder(Role.MANAGER, Role.QUALITY, Role.GENERAL_MANAGER);
    }

    @Test
    @DisplayName("Fasoncudan gelen kumaşın girişini yalnızca kalite yapar; müdür giremez")
    void shouldLetOnlyQualityRecordFabricReceipts() {
        assertThat(rolesHaving(Permission.FABRIC_RECEIPT_RECORD)).containsExactly(Role.QUALITY);
    }

    @Test
    @DisplayName("Fasoncudan depoya iplik dönüşünü yalnızca depo kaydeder; müdür ve admin kaydedemez")
    void shouldLetOnlyWarehouseRecordYarnReturns() {
        assertThat(rolesHaving(Permission.YARN_RETURN_RECORD)).containsExactly(Role.WAREHOUSE);
    }

    @Test
    @DisplayName("Merkez stoğa iplik girişini depo, kumaş girişini kalite yapar; müdürler yalnız stoğu görür")
    void shouldSplitCentralStockPermissions() {
        assertThat(rolesHaving(Permission.CENTRAL_STOCK_RECORD)).containsExactly(Role.WAREHOUSE);
        assertThat(rolesHaving(Permission.CENTRAL_PRODUCTION_RECORD)).containsExactly(Role.QUALITY);
        assertThat(rolesHaving(Permission.CENTRAL_STOCK_VIEW))
                .containsExactlyInAnyOrder(Role.WAREHOUSE, Role.QUALITY, Role.MANAGER, Role.GENERAL_MANAGER);
    }

    @Test
    @DisplayName("Uyuşmazlığı yalnızca kalite kapatır")
    void shouldAllowOnlyQualityToResolveDiscrepancy() {
        // when
        List<Role> resolvers = rolesHaving(Permission.DISCREPANCY_RESOLVE);

        // then
        assertThat(resolvers).containsExactly(Role.QUALITY);
    }

    @Test
    @DisplayName("Çıkış tartısını yalnızca depo girer; sevkiyatı müdür tamamlar")
    void shouldSeparateDispatchAndCompletion() {
        // when / then
        assertThat(rolesHaving(Permission.DISPATCH_RECORD)).containsExactly(Role.WAREHOUSE);
        assertThat(rolesHaving(Permission.SHIPMENT_COMPLETE)).containsExactly(Role.MANAGER);
        // A3: depo sevkiyat açamaz; yalnızca müdür açar.
        assertThat(rolesHaving(Permission.SHIPMENT_CREATE)).containsExactly(Role.MANAGER);
    }

    @Test
    @DisplayName("Yönetici yalnızca sistem yönetimi izinlerine sahiptir; iş ekranlarını göremez")
    void shouldLimitAdminToSystemAdministration() {
        assertThat(RolePermissionMatrix.permissionsOf(List.of(Role.ADMIN)))
                .containsExactlyInAnyOrder(
                        Permission.MASTER_DATA_VIEW,
                        Permission.USER_MANAGE,
                        Permission.SETTINGS_MANAGE,
                        Permission.PERMISSION_MANAGE,
                        Permission.AUDIT_VIEW);
    }

    @Test
    @DisplayName("Yalnızca yöneticide olan izinler başka rolün varsayılanında yoktur; düzenlenebilir roller sabittir")
    void shouldKeepAdminOnlyPermissionsWithAdmin() {
        assertThat(RolePermissionMatrix.ADMIN_ONLY)
                .containsExactlyInAnyOrder(
                        Permission.USER_MANAGE, Permission.SETTINGS_MANAGE, Permission.PERMISSION_MANAGE);
        assertThat(RolePermissionMatrix.EDITABLE_ROLES)
                .containsExactlyInAnyOrder(Role.WAREHOUSE, Role.QUALITY, Role.MANAGER, Role.GENERAL_MANAGER);
        for (Permission locked : RolePermissionMatrix.ADMIN_ONLY) {
            assertThat(rolesHaving(locked)).containsExactly(Role.ADMIN);
        }
    }

    @Test
    @DisplayName("Birden fazla rolün izinleri birleşir")
    void shouldUnionPermissionsOfRoles() {
        // when
        Set<Permission> permissions = RolePermissionMatrix.permissionsOf(List.of(Role.WAREHOUSE, Role.QUALITY));

        // then
        assertThat(permissions).contains(Permission.DISPATCH_RECORD, Permission.DISCREPANCY_RESOLVE);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Her rolün en az bir izni vardır")
    void shouldGiveEveryRoleAtLeastOnePermission(Role role) {
        // when / then
        assertThat(RolePermissionMatrix.permissionsOf(List.of(role))).isNotEmpty();
    }

    private static List<Role> rolesHaving(Permission permission) {
        return Arrays.stream(Role.values())
                .filter(role ->
                        RolePermissionMatrix.permissionsOf(List.of(role)).contains(permission))
                .toList();
    }
}
