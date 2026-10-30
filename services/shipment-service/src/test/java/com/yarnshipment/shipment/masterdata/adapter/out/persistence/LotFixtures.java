package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Parti adaptör testlerinin ortak lokasyon, ürün ve kalite kayıtları. */
final class LotFixtures {

    static final Instant LOCKED_AT = Instant.parse("2026-10-30T10:00:00Z");
    static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-0000000a1001");
    static final UUID PRODUCT = UUID.fromString("0192f0a0-0000-7000-8000-0000000a1002");
    static final UUID GRADE = UUID.fromString("0192f0a0-0000-7000-8000-0000000a1003");
    static final LotDetails DETAILS = new LotDetails("t-1", PRODUCT, GRADE, LocalDate.parse("2026-10-29"));

    private LotFixtures() {}

    /** Lokasyon, ürün ve kalite sınıfını tablo sahibi olarak ekler; tekrar çağrılabilir. */
    static void givenReferences(JdbcTemplate owner) {
        owner.update(
                "INSERT INTO md_site (id, name, created_at, created_by) VALUES (?, 'Test', now(), ?)"
                        + " ON CONFLICT DO NOTHING",
                SITE,
                ACTOR);
        owner.update(
                "INSERT INTO md_product (id, code, name, created_at, created_by)"
                        + " VALUES (?, 'LOT-P', 'İplik', now(), ?) ON CONFLICT DO NOTHING",
                PRODUCT,
                ACTOR);
        owner.update(
                "INSERT INTO md_quality_grade (id, code, name, created_at, created_by)"
                        + " VALUES (?, 'LOT-A', 'A', now(), ?) ON CONFLICT DO NOTHING",
                GRADE,
                ACTOR);
    }
}
