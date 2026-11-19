package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.in.fire.FireInvoicePage;
import com.yarnshipment.shipment.application.port.in.fire.ListFireInvoicesQuery;
import com.yarnshipment.shipment.application.port.out.ledger.FireInvoiceListPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Component;

/** Girilmiş faturaların tüm sevkiyatlar boyunca JDBC okuması (fire_invoice eklemelidir, salt okunur). */
@Component
final class FireInvoiceListAdapter implements FireInvoiceListPort {

    private static final String FROM = " FROM fire_invoice i JOIN shipment s ON s.id = i.shipment_id";
    private static final String WHERE = """
             WHERE (CAST(:fromDate AS date) IS NULL OR i.invoiced_on >= CAST(:fromDate AS date))
               AND (CAST(:toDate AS date) IS NULL OR i.invoiced_on <= CAST(:toDate AS date))
               AND (CAST(:subcontractor AS uuid) IS NULL OR s.subcontractor_id = CAST(:subcontractor AS uuid))""";

    private final JdbcClient jdbc;

    FireInvoiceListAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FireInvoicePage.Row> findInvoices(ListFireInvoicesQuery query) {
        return bind(
                        jdbc.sql("SELECT i.invoice_no, i.invoiced_on, i.kg, s.id AS shipment_id, s.shipment_no,"
                                + " s.subcontractor_id, s.subcontractor_title" + FROM + WHERE
                                + " ORDER BY i.invoiced_on DESC, i.created_at DESC, i.id LIMIT :limit OFFSET :offset"),
                        query)
                .param("limit", query.size())
                .param("offset", (long) query.page() * query.size())
                .query((rs, i) -> new FireInvoicePage.Row(
                        rs.getString("invoice_no"),
                        rs.getDate("invoiced_on").toLocalDate(),
                        rs.getObject("shipment_id", UUID.class),
                        rs.getString("shipment_no"),
                        rs.getObject("subcontractor_id", UUID.class),
                        rs.getString("subcontractor_title"),
                        rs.getBigDecimal("kg")))
                .list();
    }

    @Override
    public long countInvoices(ListFireInvoicesQuery query) {
        return bind(jdbc.sql("SELECT count(*)" + FROM + WHERE), query)
                .query(Long.class)
                .single();
    }

    @Override
    public BigDecimal sumInvoiceKg(ListFireInvoicesQuery query) {
        return bind(jdbc.sql("SELECT coalesce(sum(i.kg), 0)" + FROM + WHERE), query)
                .query(BigDecimal.class)
                .single();
    }

    private static StatementSpec bind(StatementSpec spec, ListFireInvoicesQuery query) {
        return spec.param("fromDate", query.from())
                .param("toDate", query.to())
                .param("subcontractor", query.subcontractorId() == null ? null : query.subcontractorId().toString());
    }
}
