package com.yarnshipment.bag.adapter.out.persistence;

import com.yarnshipment.bag.application.port.out.LotQualityLockPort;
import com.yarnshipment.bag.domain.model.LotQualityLock;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link LotQualityLockPort}'un JDBC uygulaması. Tablo yalnızca eklenir; güncelleme ve silme trigger'la reddedilir. */
@Component
final class LotQualityLockAdapter implements LotQualityLockPort {

    private static final String INSERT = """
            INSERT INTO lot_quality_lock (lot_id, site_id, lot_no, product_id, quality_grade_code, locked_at,
                                          created_at)
            VALUES (:lotId, :siteId, :lotNo, :productId, :qualityGradeCode, :lockedAt, :createdAt)
            ON CONFLICT (lot_id) DO NOTHING""";

    private final JdbcClient jdbc;

    LotQualityLockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(LotQualityLock lock, Instant recordedAt) {
        return jdbc.sql(INSERT)
                        .param("lotId", lock.lotId())
                        .param("siteId", lock.siteId())
                        .param("lotNo", lock.lotNo())
                        .param("productId", lock.productId())
                        .param("qualityGradeCode", lock.qualityGradeCode())
                        .param("lockedAt", Timestamp.from(lock.lockedAt()))
                        .param("createdAt", Timestamp.from(recordedAt))
                        .update()
                == 1;
    }
}
