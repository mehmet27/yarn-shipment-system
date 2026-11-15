package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Rol izinlerinin kısa süreli önbelleği; her istekte veritabanına gidilmesin diye. Bu uygulamada yapılan değişiklikte
 * hemen ({@link #invalidate()}), başka kopyada en geç süre dolunca yenilenir.
 */
@Component
final class RolePermissionCache {

    private static final Duration TTL = Duration.ofSeconds(5);

    private final Clock clock;
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    RolePermissionCache(Clock clock) {
        this.clock = clock;
    }

    Map<Role, Set<Permission>> get(Supplier<Map<Role, Set<Permission>>> loader) {
        Snapshot seen = snapshot;
        Instant now = clock.instant();
        if (seen.loadedAt().plus(TTL).isAfter(now)) {
            return seen.permissions();
        }
        Snapshot fresh = new Snapshot(now, loader.get());
        snapshot = fresh;
        return fresh.permissions();
    }

    void invalidate() {
        snapshot = Snapshot.EMPTY;
    }

    private record Snapshot(Instant loadedAt, Map<Role, Set<Permission>> permissions) {

        static final Snapshot EMPTY = new Snapshot(Instant.MIN, Map.of());
    }
}
