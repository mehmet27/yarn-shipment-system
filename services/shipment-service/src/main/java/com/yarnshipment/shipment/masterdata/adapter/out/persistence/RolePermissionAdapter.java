package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.web.platform.AuditActors;
import com.yarnshipment.shipment.masterdata.application.port.out.RolePermissionPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** {@link RolePermissionPort}'un JDBC uygulaması; iz {@code audit_local} trigger'ından düşer. */
@Component
final class RolePermissionAdapter implements RolePermissionPort {

    private static final String SELECT = "SELECT role, permission FROM role_permission";
    private static final String INSERT = """
            INSERT INTO role_permission (role, permission, updated_at, updated_by)
            VALUES (:role, :permission, :at, :actor) ON CONFLICT DO NOTHING""";
    private static final String DELETE = "DELETE FROM role_permission WHERE role = :role AND permission = :permission";

    private final JdbcClient jdbc;
    private final Clock clock;
    private final RolePermissionCache cache;

    RolePermissionAdapter(JdbcClient jdbc, Clock clock, RolePermissionCache cache) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.cache = cache;
    }

    @Override
    public Map<Role, Set<Permission>> findAll() {
        Map<Role, Set<Permission>> result = new EnumMap<>(Role.class);
        jdbc.sql(SELECT)
                .query((rs, row) -> new String[] {rs.getString("role"), rs.getString("permission")})
                .list()
                .forEach(pair -> add(result, pair[0], pair[1]));
        return result;
    }

    @Override
    public void replace(UUID actorId, Role role, Set<Permission> permissions) {
        AuditActors.actAs(jdbc, actorId);
        Set<Permission> current = findAll().getOrDefault(role, Set.of());
        for (Permission permission : current) {
            if (!permissions.contains(permission)) {
                jdbc.sql(DELETE)
                        .param("role", role.name())
                        .param("permission", permission.name())
                        .update();
            }
        }
        for (Permission permission : permissions) {
            if (!current.contains(permission)) {
                jdbc.sql(INSERT)
                        .param("role", role.name())
                        .param("permission", permission.name())
                        .param("at", Timestamp.from(clock.instant()))
                        .param("actor", actorId)
                        .update();
            }
        }
        invalidateAfterCommit();
    }

    /** Önbellek işlem tamamlanınca silinir; yoksa başka istek eski değeri yeniden yükleyebilirdi. */
    private void invalidateAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cache.invalidate();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cache.invalidate();
            }
        });
    }

    private static void add(Map<Role, Set<Permission>> result, String roleName, String permissionName) {
        Permission permission = parse(permissionName);
        // Koddan kaldırılmış bir izin adı kayıtta kalmış olabilir; yok sayılır.
        if (permission != null) {
            result.computeIfAbsent(Role.valueOf(roleName), r -> EnumSet.noneOf(Permission.class))
                    .add(permission);
        }
    }

    private static @Nullable Permission parse(String name) {
        try {
            return Permission.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
