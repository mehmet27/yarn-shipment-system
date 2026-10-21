package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.adapter.out.persistence.ShipmentTransitionWriter.Transition;
import com.yarnshipment.shipment.application.port.out.DeliveryCommandPort;
import com.yarnshipment.shipment.domain.model.DeliveryDocument;
import com.yarnshipment.shipment.domain.model.DeliveryReceipt;
import com.yarnshipment.shipment.domain.model.DeliveryReceived;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link DeliveryCommandPort}'un JDBC uygulaması. Tutanak okutmalarla birlikte yazıldıktan sonra
 * {@code submitted_at} doldurularak kilitlenir; okutulan her anahtar,
 * listede olmasa da {@code matched = false} ile saklanır.
 */
@Component
final class DeliveryCommandAdapter implements DeliveryCommandPort {

    private static final String DELIVERY = "DELIVERY";
    private static final String COUNT_RECEIPTS = "SELECT count(*) FROM delivery_receipt WHERE shipment_id = ?";
    private static final String EXPECTED_KEYS =
            "SELECT unique_key FROM shipment_bag WHERE shipment_id = ? AND removed_at IS NULL";
    private static final String INSERT_RECEIPT = """
            INSERT INTO delivery_receipt (id, shipment_id, received_by, received_at, signed_doc_evidence_id,
                                          doc_shipment_no, doc_date, doc_net_kg, doc_bag_count, submitted_at,
                                          created_at, created_by)
            VALUES (:id, :shipmentId, :actor, :at, :evidenceId, :docNo, :docDate, :docNetKg, :docBagCount, NULL,
                    :at, :actor)""";
    private static final String SUBMIT_RECEIPT = "UPDATE delivery_receipt SET submitted_at = ? WHERE id = ?";
    private static final String INSERT_SCAN = """
            INSERT INTO delivery_scan (id, delivery_receipt_id, scanned_key, matched, scan_method, scanned_at,
                                       created_at, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";
    private static final String INSERT_WEIGHING = """
            INSERT INTO weighing (id, shipment_id, side, gross_kg, tare_kg, bag_count, weighed_at, entered_by,
                                  created_at, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";
    private static final String INSERT_SEAL = """
            INSERT INTO seal (id, shipment_id, side, seal_no, seal_condition, recorded_by, recorded_at, created_at,
                              created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private final JdbcClient jdbc;
    private final ShipmentTransitionWriter transitions;

    DeliveryCommandAdapter(JdbcClient jdbc, ShipmentTransitionWriter transitions) {
        this.jdbc = jdbc;
        this.transitions = transitions;
    }

    @Override
    public boolean hasDeliveryReceipt(UUID shipmentId) {
        long count =
                jdbc.sql(COUNT_RECEIPTS).param(shipmentId).query(Long.class).single();
        return count > 0;
    }

    @Override
    public void recordDelivery(DeliveryReceipt receipt) {
        Transition transition = new Transition(
                receipt.shipmentId(),
                receipt.fromStatus(),
                receipt.toStatus(),
                receipt.receivedBy(),
                Timestamp.from(receipt.receivedAt()));
        transitions.actAs(receipt.receivedBy());
        transitions.changeStatus(transition);
        UUID receiptId = transitions.nextId();
        insertReceipt(receiptId, receipt, transition.at());
        List<String> expectedKeys = expectedKeys(receipt.shipmentId());
        insertScans(receiptId, receipt, Set.copyOf(expectedKeys), transition.at());
        insertWeighingAndSeal(receipt, transition.at());
        jdbc.sql(SUBMIT_RECEIPT).params(transition.at(), receiptId).update();
        transitions.publish(transition, DeliveryReceived.TYPE, DeliveryReceived.of(receipt, receiptId, expectedKeys));
    }

    /** {@code unique_key} NOT NULL olduğu için boş değer gelmez. */
    private List<String> expectedKeys(UUID shipmentId) {
        return jdbc.sql(EXPECTED_KEYS).param(shipmentId).query(String.class).list().stream()
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull)
                .toList();
    }

    private void insertReceipt(UUID receiptId, DeliveryReceipt receipt, Timestamp at) {
        DeliveryDocument document = receipt.measurement().document();
        jdbc.sql(INSERT_RECEIPT)
                .param("id", receiptId)
                .param("shipmentId", receipt.shipmentId())
                .param("actor", receipt.receivedBy())
                .param("at", at)
                .param("evidenceId", document.signedDocumentEvidenceId())
                .param("docNo", document.shipmentNo())
                .param("docDate", document.date())
                .param("docNetKg", document.netKg())
                .param("docBagCount", document.bagCount())
                .update();
    }

    private void insertScans(UUID receiptId, DeliveryReceipt receipt, Set<String> expectedKeys, Timestamp at) {
        String method = receipt.measurement().scanMethod().name();
        for (String key : new HashSet<>(receipt.measurement().scannedKeys())) {
            jdbc.sql(INSERT_SCAN)
                    .params(
                            transitions.nextId(),
                            receiptId,
                            key,
                            expectedKeys.contains(key),
                            method,
                            at,
                            at,
                            receipt.receivedBy())
                    .update();
        }
    }

    /** Mühür yoksa (MISSING) numara boş dize olarak saklanır; kolon NOT NULL'dır. */
    private void insertWeighingAndSeal(DeliveryReceipt receipt, Timestamp at) {
        UUID actor = receipt.receivedBy();
        jdbc.sql(INSERT_WEIGHING)
                .params(
                        transitions.nextId(),
                        receipt.shipmentId(),
                        DELIVERY,
                        receipt.weighing().grossKg(),
                        receipt.weighing().tareKg(),
                        receipt.weighing().bagCount(),
                        at,
                        actor,
                        at,
                        actor)
                .update();
        String sealNo = receipt.measurement().sealNo();
        jdbc.sql(INSERT_SEAL)
                .params(
                        transitions.nextId(),
                        receipt.shipmentId(),
                        DELIVERY,
                        sealNo == null ? "" : sealNo,
                        receipt.measurement().sealCondition().name(),
                        actor,
                        at,
                        at,
                        actor)
                .update();
    }
}
