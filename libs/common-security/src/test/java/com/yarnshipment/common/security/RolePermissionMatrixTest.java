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
    @DisplayName("Yönetici salt okur: hiçbir yazma izni yoktur")
    void shouldGiveManagerOnlyReadPermissions() {
        // when
        Set<Permission> permissions = RolePermissionMatrix.permissionsOf(List.of(Role.MANAGER));

        // then
        assertThat(permissions).isNotEmpty().noneMatch(Permission::isWrite);
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
    @DisplayName("Karşılaştırmayı yalnızca sistem hesabı tetikler")
    void shouldAllowOnlySystemToRunComparison() {
        // when / then
        assertThat(rolesHaving(Permission.COMPARISON_RUN)).containsExactly(Role.SYSTEM);
    }

    @Test
    @DisplayName("Çıkış tartısını yalnızca depo, teslim tartısını yalnızca fasoncu girer")
    void shouldSeparateDispatchAndDeliveryRecording() {
        // when / then
        assertThat(rolesHaving(Permission.DISPATCH_RECORD)).containsExactly(Role.WAREHOUSE);
        assertThat(rolesHaving(Permission.DELIVERY_RECORD)).containsExactly(Role.SUBCONTRACTOR);
        assertThat(rolesHaving(Permission.DRIVER_DECLARATION_RECORD)).containsExactly(Role.DRIVER);
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
