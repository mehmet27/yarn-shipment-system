package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.out.RolePermissionPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Yetkiler ekranı kuralları: yönetici ve yalnızca yöneticide olan izinler sabittir. */
class RolePermissionServiceTest {

    private final RolePermissionPort port = mock(RolePermissionPort.class);
    private final RolePermissionService service = new RolePermissionService(port);
    private final Actor admin = TestTokens.actor("admin");

    @Test
    @DisplayName("Rolün izinleri kaydedilir")
    void shouldSaveAndPublish() {
        Set<Permission> wanted = EnumSet.of(Permission.SHIPMENT_VIEW, Permission.REPORT_VIEW);

        service.saveRolePermissions(admin, Role.WAREHOUSE, wanted);

        verify(port).replace(admin.userId(), Role.WAREHOUSE, wanted);
    }

    @Test
    @DisplayName("Yönetici rolünün izinleri değiştirilemez")
    void shouldRejectAdminRole() {
        assertThatThrownBy(() -> service.saveRolePermissions(admin, Role.ADMIN, Set.of(Permission.SHIPMENT_VIEW)))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.resetRolePermissions(admin, Role.ADMIN))
                .isInstanceOf(InvalidMasterDataException.class);
        verify(port, never()).replace(any(), any(), any());
    }

    @Test
    @DisplayName("Yalnızca yöneticide olan izinler başka role verilemez")
    void shouldRejectAdminOnlyPermissions() {
        for (Permission locked : RolePermissionMatrix.ADMIN_ONLY) {
            assertThatThrownBy(() -> service.saveRolePermissions(admin, Role.MANAGER, Set.of(locked)))
                    .isInstanceOf(InvalidMasterDataException.class);
        }
        verify(port, never()).replace(any(), any(), any());
    }

    @Test
    @DisplayName("Varsayılana dönüş rolün koddaki izinlerini yazar")
    void shouldResetToDefaults() {
        service.resetRolePermissions(admin, Role.MANAGER);

        verify(port).replace(admin.userId(), Role.MANAGER, RolePermissionMatrix.defaultsOf(Role.MANAGER));
    }

    @Test
    @DisplayName("Liste: yönetici koddan, düzenlenebilir roller kayıttan gelir; kaydı olmayan rol boştur")
    void shouldListFixedFromCodeAndEditableFromStore() {
        when(port.findAll()).thenReturn(Map.of(Role.WAREHOUSE, Set.of(Permission.SHIPMENT_VIEW)));

        Map<Role, Set<Permission>> listed = service.listRolePermissions();

        assertThat(listed.get(Role.ADMIN)).isEqualTo(RolePermissionMatrix.defaultsOf(Role.ADMIN));
        assertThat(listed.get(Role.WAREHOUSE)).containsExactly(Permission.SHIPMENT_VIEW);
        assertThat(listed.get(Role.MANAGER)).isEmpty();
        assertThat(listed).doesNotContainKey(Role.SYSTEM);
    }
}
