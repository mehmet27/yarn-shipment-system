package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Parti sorgusunun satırını domain modeline çevirir. */
final class LotRows {

    private LotRows() {}

    static ProductionLot lot(ResultSet rs) throws SQLException {
        OffsetDateTime lockedAt = rs.getObject("quality_locked_at", OffsetDateTime.class);
        return new ProductionLot(
                rs.getObject("id", UUID.class),
                rs.getObject("site_id", UUID.class),
                new LotDetails(
                        rs.getString("lot_no"),
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("quality_grade_id", UUID.class),
                        rs.getObject("production_date", LocalDate.class)),
                new ProductionLot.Labels(
                        rs.getString("site_name"),
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getString("quality_grade_code"),
                        rs.getString("quality_grade_name")),
                lockedAt == null ? null : lockedAt.toInstant(),
                rs.getBoolean("active"));
    }
}
