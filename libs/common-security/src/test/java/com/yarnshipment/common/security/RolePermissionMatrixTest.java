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
    @DisplayName("Müdürün yazma izinleri sevkiyat yaşam döngüsü, kumaş/iplik girişi, fire, fatura ve tartı kararıdır")
    void shouldLimitManagerWrites() {
        // when
        Set<Permission> permissions = RolePermissionMatrix.permissionsOf(List.of(Role.MANAGER));

        // then
        assertThat(permissions.stream().filter(Permission::isWrite))
                .containsExactlyInAnyOrder(
                        Permission.SHIPMENT_CREATE,
                        Permission.SHIPMENT_CANCEL,
                        Permission.SHIPMENT_COMPLETE,
                        Permission.EVIDENCE_UPLOAD_CLAIM,
                        Permission.QUALITY_CLAIM_OPEN,
                        Permission.FABRIC_RECEIPT_RECORD,
                        Permission.YARN_RETURN_RECORD,
                        Permission.FIRE_RATE_EDIT,
                        Permission.INVOICE_MARK,
                        Permission.WEIGHT_MISMATCH_RESOLVE);
    }

    @Test
    @DisplayName("Genel müdür yalnızca rapor, bakiye ve ad çözmek için ana veri listesini görür")
    void shouldLimitGeneralManagerToReadOnly() {
        assertThat(RolePermissionMatrix.permissionsOf(List.of(Role.GENERAL_MANAGER)))
                .containsExactlyInAnyOrder(
                        Permission.REPORT_VIEW, Permission.BALANCE_VIEW, Permission.MASTER_DATA_VIEW);
    }

    @Test
    @DisplayName("Sevkiyatı yalnızca müdür ve admin açar; depo açamaz")
    void shouldNotAllowWarehouseToCreateShipments() {
        assertThat(rolesHaving(Permission.SHIPMENT_CREATE)).containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER);
        assertThat(rolesHaving(Permission.BALANCE_VIEW))
                .containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER, Role.QUALITY, Role.GENERAL_MANAGER);
    }

    @Test
    @DisplayName("Merkez stoğa iplik girişini depo, kumaş girişini kalite yapar; müdür yalnız stoğu görür")
    void shouldSplitCentralStockPermissions() {
        assertThat(rolesHaving(Permission.CENTRAL_STOCK_RECORD)).containsExactlyInAnyOrder(Role.ADMIN, Role.WAREHOUSE);
        assertThat(rolesHaving(Permission.CENTRAL_PRODUCTION_RECORD))
                .containsExactlyInAnyOrder(Role.ADMIN, Role.QUALITY);
        assertThat(rolesHaving(Permission.CENTRAL_STOCK_VIEW))
                .containsExactlyInAnyOrder(Role.ADMIN, Role.WAREHOUSE, Role.QUALITY, Role.MANAGER);
    }

    @Test
    @DisplayName("Uyuşmazlığı yalnızca admin ve kalite kapatır")
    void shouldAllowOnlyAdminAndQualityToResolveDiscrepancy() {
        // when
        List<Role> resolvers = rolesHaving(Permission.DISCREPANCY_RESOLVE);

        // then
        assertThat(resolvers).containsExactlyInAnyOrder(Role.ADMIN, Role.QUALITY);
    }

    @Test
    @DisplayName("Çıkış tartısını yalnızca depo girer; sevkiyatı müdür ve admin tamamlar")
    void shouldSeparateDispatchAndCompletion() {
        // when / then
        assertThat(rolesHaving(Permission.DISPATCH_RECORD)).containsExactly(Role.WAREHOUSE);
        assertThat(rolesHaving(Permission.SHIPMENT_COMPLETE)).containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER);
        // A3: depo sevkiyat açamaz; yalnızca müdür ve admin açar.
        assertThat(rolesHaving(Permission.SHIPMENT_CREATE)).containsExactlyInAnyOrder(Role.ADMIN, Role.MANAGER);
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
