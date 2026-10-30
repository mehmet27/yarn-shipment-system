package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.TestTokens;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.BagStatus;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.PackageType;
import com.yarnshipment.common.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/** Çuval üretimi ve etiket servis testlerinin ortak verisi. */
final class ProductionFixtures {

    static final UUID LOT_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    private ProductionFixtures() {}

    static Actor warehouse() {
        return TestTokens.actor("warehouse");
    }

    static LockedLot lot(UUID siteId) {
        return new LockedLot(LOT_ID, siteId, "P-2026-0001", UUID.randomUUID(), "A", CLOCK.instant(), 0);
    }

    static Bag bag(String key, BagStatus status) {
        return bagAt(TestTokens.SITE_ID, key, status);
    }

    static Bag bagAt(UUID siteId, String key, BagStatus status) {
        return new Bag(UUID.randomUUID(), siteId, key, PackageType.BAG, "P-2026-0001", "A", status, null);
    }
}
