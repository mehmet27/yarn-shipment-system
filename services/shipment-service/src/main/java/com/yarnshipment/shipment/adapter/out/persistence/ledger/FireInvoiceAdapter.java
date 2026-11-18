package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.FireInvoicePort;
import com.yarnshipment.shipment.domain.balance.BalanceMovement;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.InvoiceContext;
import com.yarnshipment.shipment.domain.balance.BalanceMovement.InvoiceShare;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Fire dosyası faturalarının JDBC uygulaması; kilolar bakiye defterine INVOICE hareketi olarak yazılır (KR-13). */
@Component
final class FireInvoiceAdapter implements FireInvoicePort {

    private static final String DISPATCHED_BY_YARN = """
            SELECT yarn_type, sum(net_kg) AS net_kg FROM balance_movement
            WHERE shipment_id = :shipmentId AND kind = 'DISPATCH'
            GROUP BY yarn_type ORDER BY min(source_ref)""";
    private static final String INSERT_INVOICE = """
            INSERT INTO fire_invoice (id, shipment_id, invoice_no, invoiced_on, note, kg, invoiced_by, created_at)
            VALUES (:id, :shipmentId, :invoiceNo, :invoicedOn, :note, :kg, :invoicedBy, :createdAt)""";
    private static final String INVOICES = """
            SELECT invoice_no, invoiced_on, kg FROM fire_invoice WHERE shipment_id = ?
            ORDER BY created_at, id""";

    private final JdbcClient jdbc;
    private final BalanceMovementWriter writer;

    FireInvoiceAdapter(JdbcClient jdbc, BalanceMovementWriter writer) {
        this.jdbc = jdbc;
        this.writer = writer;
    }

    @Override
    public Map<String, BigDecimal> dispatchedByYarn(UUID shipmentId) {
        Map<String, BigDecimal> byYarn = new LinkedHashMap<>();
        jdbc.sql(DISPATCHED_BY_YARN)
                .param("shipmentId", shipmentId)
                .query((rs, i) -> byYarn.put(rs.getString("yarn_type"), rs.getBigDecimal("net_kg")))
                .list();
        return byYarn;
    }

    @Override
    public boolean invoiceExists(UUID shipmentId, String invoiceNo) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM fire_invoice WHERE shipment_id = ? AND invoice_no = ?)")
                .params(shipmentId, invoiceNo)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void recordInvoice(InvoiceRecord invoice) {
        BigDecimal total = invoice.lines().stream().map(InvoiceShare::netKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        jdbc.sql(INSERT_INVOICE)
                .param("id", UUID.randomUUID())
                .param("shipmentId", invoice.shipmentId())
                .param("invoiceNo", invoice.invoiceNo())
                .param("invoicedOn", invoice.invoicedOn())
                .param("note", invoice.note())
                .param("kg", total)
                .param("invoicedBy", invoice.invoicedBy())
                .param("createdAt", Timestamp.from(invoice.at()))
                .update();
        InvoiceContext context = new InvoiceContext(
                invoice.subcontractorId(),
                invoice.shipmentId(),
                invoice.invoiceNo(),
                invoice.at(),
                invoice.invoicedBy());
        invoice.lines().forEach(line -> writer.append(BalanceMovement.invoice(line, context)));
    }

    @Override
    public List<InvoiceEntry> invoices(UUID shipmentId) {
        return jdbc.sql(INVOICES)
                .param(shipmentId)
                .query((rs, i) -> new InvoiceEntry(
                        rs.getString("invoice_no"), rs.getDate("invoiced_on").toLocalDate(), rs.getBigDecimal("kg")))
                .list();
    }
}
