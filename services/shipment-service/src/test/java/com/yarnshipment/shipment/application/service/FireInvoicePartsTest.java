package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.domain.fabric.FabricYarnNotInShipmentException;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import com.yarnshipment.shipment.domain.fire.InvalidFireSettlementActionException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fire oranı, fatura işareti ve kapatma kuralları ile Kırıkhan örneğinin uçtan uca hesabı. */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class FireInvoicePartsTest {

    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor manager = TestTokens.actor("manager");
    private final ScopedShipmentLoader loader = new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock));
    private final FireSettlementDetailsReader reader = new FireSettlementDetailsReader(settlements, settlements);
    private final FireInvoiceService invoiceService =
            new FireInvoiceService(loader, settlements, settlements, reader, events, clock);

    @BeforeEach
    void givenKirikhanShipment() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        settlements.put(FireSettlement.open(
                        TestShipments.SHIPMENT_ID, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("34032.000"))
                .afterFabricReceipt());
        settlements.givenTotals("0", "0", "32660");
        settlements.givenDispatched(Map.of("Pamuk 30/1", new BigDecimal("34032")));
    }

    private MarkInvoicedCommand invoice(String invoiceNo, List<YarnShare> lines) {
        return new MarkInvoicedCommand(
                manager, TestShipments.SHIPMENT_ID, invoiceNo, LocalDate.parse("2026-11-12"), null, lines);
    }

    @Test
    @DisplayName("Faturalanan kilo iplik başına yazılır; kalan kilo bitmeden dosya açık kalır, bitince INVOICED olur")
    void shouldInvoiceInPartsAndKeepFileOpenUntilRemainingIsDone() {
        // given: faturalanacak 10,720 kg
        MarkInvoicedCommand first = invoice("F-1", List.of(new YarnShare("Pamuk 30/1", new BigDecimal("4"))));

        // when
        FireSettlementDetails partial = invoiceService.markInvoiced(first);

        // then
        assertThat(partial.settlement().status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(partial.calculation().invoicedKg()).isEqualByComparingTo("4");
        assertThat(partial.calculation().remainingKg()).isEqualByComparingTo("6.720");
        assertThat(settlements.recorded()).singleElement().satisfies(record -> {
            assertThat(record.lines()).singleElement().satisfies(line -> {
                assertThat(line.yarnType()).isEqualTo("Pamuk 30/1");
                assertThat(line.netKg()).isEqualByComparingTo("4");
            });
        });

        // when: kalan, iplik belirtilmeden faturalanır (iplik oranıyla paylaştırılır)
        FireSettlementDetails rest = invoiceService.markInvoiced(invoice("F-2", List.of()));

        // then
        assertThat(rest.settlement().status()).isEqualTo(FireSettlementStatus.INVOICED);
        assertThat(rest.settlement().invoiceNo()).isEqualTo("F-2");
        assertThat(rest.calculation().remainingKg()).isEqualByComparingTo("0");
        assertThat(rest.invoices()).extracting(entry -> entry.invoiceNo()).containsExactly("F-1", "F-2");
        assertThat(events.names).containsExactly("fireInvoiced", "fireInvoiced");
    }

    @Test
    @DisplayName("Kalandan fazla kilo faturalanamaz ve sevkiyatta olmayan iplik reddedilir")
    void shouldRejectInvoiceOverRemainingOrUnknownYarn() {
        MarkInvoicedCommand tooMuch = invoice("F-1", List.of(new YarnShare("Pamuk 30/1", new BigDecimal("11"))));
        MarkInvoicedCommand unknown = invoice("F-2", List.of(new YarnShare("Başka iplik", BigDecimal.ONE)));

        assertThatThrownBy(() -> invoiceService.markInvoiced(tooMuch))
                .isInstanceOf(InvalidFireSettlementActionException.class);
        assertThatThrownBy(() -> invoiceService.markInvoiced(unknown))
                .isInstanceOf(FabricYarnNotInShipmentException.class);
        assertThat(settlements.recorded()).isEmpty();
    }
}
