package com.yarnshipment.reporting.adapter.out.persistence;

import com.yarnshipment.reporting.application.port.out.ReadModelPort;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.sql.Timestamp;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link ReadModelPort}'un JDBC uygulaması. Durum güncellemeleri geriye gitmez: tamamlanmış ya da iptal edilmiş
 * sevkiyatın durumu, geç gelen olayla değişmez. Tamamlanan sevkiyatta fasoncuya ulaşan kilo çıkış netidir.
 */
@Component
final class ReadModelAdapter implements ReadModelPort {

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
                                   SET dispatch_net_kg = :netKg, dispatched_at = :at,
                                       status = CASE WHEN status = 'DRAFT'
                                                     THEN 'AWAITING_DELIVERY' ELSE status END
                                 WHERE shipment_id = :id""")
                        .param("id", dispatched.shipmentId())
                        .param("netKg", dispatched.netKg())
                        .param("at", Timestamp.from(dispatched.dispatchedAt()))
                        .update()
                == 1;
    }

    @Override
    public boolean saveCompleted(ReadModelChange.ShipmentCompleted completed) {
        return jdbc.sql("""
                                UPDATE shipment_view
                                   SET status = 'COMPLETED', completed_at = :at, delivery_net_kg = dispatch_net_kg
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
        jdbc.sql("""
                        UPDATE shipment_view
                           SET open_discrepancy_count = open_discrepancy_count + 1, had_discrepancy = true
                         WHERE shipment_id = :id""").param("id", opened.shipmentId()).update();
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
