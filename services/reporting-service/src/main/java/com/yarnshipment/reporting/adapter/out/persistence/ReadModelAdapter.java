package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import com.yarnshipment.reporting.domain.model.ShipmentParties;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link ReadModelPort}'un JDBC uygulaması. Durum güncellemeleri geriye gitmez: tamamlanmış ya da iptal edilmiş
 * sevkiyatın durumu, farklı konulardan geç gelen olayla değişmez.
 */
@Component
final class ReadModelAdapter implements ReadModelPort {

    private static final String FINAL_STATUSES = "('COMPLETED', 'CANCELLED')";

    private final JdbcClient jdbc;

    ReadModelAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertShipment(ReadModelChange.ShipmentCreated created) {
        jdbc.sql("""
                        INSERT INTO shipment_view (shipment_id, site_id, shipment_no, subcontractor_id, status,
                                                   created_at)
                        VALUES (:id, :siteId, :shipmentNo, :subcontractorId, 'DRAFT', :createdAt)
                        ON CONFLICT (shipment_id) DO NOTHING""")
                .param("id", created.shipmentId())
                .param("siteId", created.siteId())
                .param("shipmentNo", created.shipmentNo())
                .param("subcontractorId", created.subcontractorId())
                .param("createdAt", Timestamp.from(created.createdAt()))
                .update();
    }

    @Override
    public boolean saveDispatched(ReadModelChange.ShipmentDispatched dispatched) {
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET driver_id = :driverId, dispatched_at = :at,
                                       status = CASE WHEN status IN ('DRAFT', 'LOADED')
                                                     THEN 'IN_TRANSIT' ELSE status END
                                 WHERE shipment_id = :id""")
                        .param("id", dispatched.shipmentId())
                        .param("driverId", dispatched.driverId())
                        .param("at", Timestamp.from(dispatched.dispatchedAt()))
                        .update()
                == 1;
    }

    @Override
    public boolean saveArrived(ReadModelChange.ShipmentArrived arrived) {
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET driver_id = coalesce(driver_id, :driverId),
                                       status = CASE WHEN status IN ('DRAFT', 'LOADED', 'IN_TRANSIT')
                                                     THEN 'AWAITING_DELIVERY' ELSE status END
                                 WHERE shipment_id = :id""")
                        .param("id", arrived.shipmentId())
                        .param("driverId", arrived.driverId())
                        .update()
                == 1;
    }

    @Override
    public boolean saveCompleted(ReadModelChange.ShipmentCompleted completed) {
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET status = 'COMPLETED', completed_at = :at
                                 WHERE shipment_id = :id AND status <> 'CANCELLED'""")
                        .param("id", completed.shipmentId())
                        .param("at", Timestamp.from(completed.completedAt()))
                        .update()
                == 1;
    }

    @Override
    public boolean saveCancelled(ReadModelChange.ShipmentCancelled cancelled) {
        return jdbc.sql("UPDATE shipment_view SET status = 'CANCELLED' "
                                + "WHERE shipment_id = :id AND status <> 'COMPLETED'")
                        .param("id", cancelled.shipmentId())
                        .update()
                == 1;
    }

    @Override
    public boolean saveDelivery(ReadModelChange.DeliveryReceived delivery) {
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET dispatch_net_kg = :dispatchNet, delivery_net_kg = :deliveryNet,
                                       status = CASE WHEN status IN ('IN_REVIEW', 'COMPLETED', 'CANCELLED')
                                                     THEN status ELSE 'CHECKING' END
                                 WHERE shipment_id = :id""")
                        .param("id", delivery.shipmentId())
                        .param("dispatchNet", delivery.dispatchNetKg())
                        .param("deliveryNet", delivery.deliveryNetKg())
                        .update()
                == 1;
    }

    @Override
    public boolean saveComparison(ReadModelChange.ComparisonCompleted comparison) {
        boolean discrepant = comparison.discrepancyCount() > 0;
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET band = :band, diff_kg = :diffKg, diff_pct = :diffPct, compared_at = :at,
                                       had_discrepancy = :discrepant,
                                       status = CASE WHEN :discrepant AND status NOT IN %s
                                                     THEN 'IN_REVIEW' ELSE status END
                                 WHERE shipment_id = :id""".formatted(FINAL_STATUSES))
                        .param("id", comparison.shipmentId())
                        .param("band", comparison.band())
                        .param("diffKg", comparison.diffKg())
                        .param("diffPct", comparison.diffPct())
                        .param("at", Timestamp.from(comparison.comparedAt()))
                        .param("discrepant", discrepant)
                        .update()
                == 1;
    }

    @Override
    public Optional<ShipmentParties> findParties(UUID shipmentId) {
        return jdbc.sql("SELECT site_id, driver_id, subcontractor_id FROM shipment_view WHERE shipment_id = :id")
                .param("id", shipmentId)
                .query((rs, row) -> new ShipmentParties(
                        rs.getObject("site_id", UUID.class),
                        rs.getObject("driver_id", UUID.class),
                        rs.getObject("subcontractor_id", UUID.class)))
                .optional();
    }

    @Override
    public void addPairComparison(PairComparison comparison) {
        boolean discrepant = comparison.discrepancyCount() > 0;
        jdbc.sql("""
                        INSERT INTO pair_daily_stats (day, site_id, driver_id, subcontractor_id, shipment_count,
                                                      discrepant_shipment_count, discrepancy_count, total_diff_kg,
                                                      created_at)
                        VALUES (:day, :siteId, :driverId, :subcontractorId, 1, :discrepantShipments, :discrepancies,
                                :absDiff, :at)
                        ON CONFLICT (day, site_id, driver_id, subcontractor_id) DO UPDATE
                           SET shipment_count = pair_daily_stats.shipment_count + 1,
                               discrepant_shipment_count = pair_daily_stats.discrepant_shipment_count
                                                           + EXCLUDED.discrepant_shipment_count,
                               discrepancy_count = pair_daily_stats.discrepancy_count + EXCLUDED.discrepancy_count,
                               total_diff_kg = pair_daily_stats.total_diff_kg + EXCLUDED.total_diff_kg,
                               updated_at = EXCLUDED.created_at,
                               version = pair_daily_stats.version + 1""")
                .param("day", comparison.day())
                .param("siteId", comparison.siteId())
                .param("driverId", comparison.driverId())
                .param("subcontractorId", comparison.subcontractorId())
                .param("discrepantShipments", discrepant ? 1 : 0)
                .param("discrepancies", comparison.discrepancyCount())
                .param("absDiff", comparison.absoluteDiffKg())
                .param("at", Timestamp.from(comparison.comparedAt()))
                .update();
    }

    @Override
    public void insertDiscrepancy(ReadModelChange.DiscrepancyOpened opened) {
        int inserted = jdbc.sql("""
                        INSERT INTO discrepancy_view (discrepancy_id, discrepancy_no, shipment_id, site_id, type,
                                                      severity, due_at, created_at)
                        VALUES (:id, :no, :shipmentId, :siteId, :type, :severity, :dueAt, :createdAt)
                        ON CONFLICT (discrepancy_id) DO NOTHING""")
                .param("id", opened.discrepancyId())
                .param("no", opened.discrepancyNo())
                .param("shipmentId", opened.shipmentId())
                .param("siteId", opened.siteId())
                .param("type", opened.type())
                .param("severity", opened.severity())
                .param("dueAt", Timestamp.from(opened.dueAt()))
                .param("createdAt", Timestamp.from(opened.openedAt()))
                .update();
        if (inserted == 0) {
            return;
        }
        jdbc.sql("UPDATE shipment_view SET open_discrepancy_count = open_discrepancy_count + 1 WHERE shipment_id = :id")
                .param("id", opened.shipmentId())
                .update();
    }

    @Override
    public void saveDiscrepancyResolved(ReadModelChange.DiscrepancyResolved resolved) {
        jdbc.sql("UPDATE discrepancy_view SET resolved_at = :at WHERE discrepancy_id = :id AND resolved_at IS NULL")
                .param("id", resolved.discrepancyId())
                .param("at", Timestamp.from(resolved.resolvedAt()))
                .update();
        jdbc.sql("UPDATE shipment_view SET open_discrepancy_count = :remaining WHERE shipment_id = :id")
                .param("id", resolved.shipmentId())
                .param("remaining", resolved.remainingOpen())
                .update();
    }
}
