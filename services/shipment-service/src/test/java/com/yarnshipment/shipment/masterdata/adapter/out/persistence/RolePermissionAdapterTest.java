package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.shipment.adapter.out.persistence.PostgresTestDatabase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Rol izinleri {@code role_permission} tablosunda; ilk içerik koddaki varsayılandır (gerçek PostgreSQL). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RolePermissionAdapterTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d002");

    @Autowired
    private JdbcClient jdbc;

    private RolePermissionAdapter adapter;

    @BeforeEach
    void createAdapter() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZoneOffset.UTC);
        adapter = new RolePermissionAdapter(jdbc, clock, new RolePermissionCache(clock));
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Kurulumdaki kayıtlar koddaki varsayılan matrisle aynıdır")
    void shouldStartWithDefaults() {
        for (Role role : RolePermissionMatrix.EDITABLE_ROLES) {
            assertThat(adapter.findAll().get(role)).isEqualTo(RolePermissionMatrix.defaultsOf(role));
        }
    }

    @Test
    @DisplayName("Rolün izin kümesi eşitlenir: eksik eklenir, fazla silinir; diğer roller etkilenmez")
    void shouldReplaceOnlyGivenRole() {
        var before = adapter.findAll().get(Role.QUALITY);

        adapter.replace(ACTOR, Role.WAREHOUSE, EnumSet.of(Permission.SHIPMENT_VIEW, Permission.REPORT_VIEW));

        assertThat(adapter.findAll().get(Role.WAREHOUSE))
                .containsExactlyInAnyOrder(Permission.SHIPMENT_VIEW, Permission.REPORT_VIEW);
        assertThat(adapter.findAll().get(Role.QUALITY)).isEqualTo(before);
    }

    @Test
    @DisplayName("Boş küme rolün tüm izinlerini kaldırır")
    void shouldClearAllPermissions() {
        adapter.replace(ACTOR, Role.GENERAL_MANAGER, EnumSet.noneOf(Permission.class));

        assertThat(adapter.findAll()).doesNotContainKey(Role.GENERAL_MANAGER);
    }
}
