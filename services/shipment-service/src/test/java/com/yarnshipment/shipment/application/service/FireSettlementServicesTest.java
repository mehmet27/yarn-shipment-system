package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fire.ChangeFireRateCommand;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementDetails;
import com.yarnshipment.shipment.application.port.in.fire.FireSettlementQuery;
import com.yarnshipment.shipment.application.port.in.fire.MarkInvoicedCommand;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementClosedException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import com.yarnshipment.shipment.domain.fire.InvalidFireSettlementActionException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fire oranı, fatura işareti ve kapatma kuralları ile Kırıkhan örneğinin uçtan uca hesabı. */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class FireSettlementServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor manager = TestTokens.actor("manager");
    private final ScopedShipmentLoader loader = new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock));
    private final FireSettlementDetailsReader reader = new FireSettlementDetailsReader(settlements);
    private final FireRateService rateService = new FireRateService(loader, settlements, reader, events, clock);
    private final FireInvoiceService invoiceService =
            new FireInvoiceService(loader, settlements, reader, events, clock);
    private final FireSettlementQueryService queryService = new FireSettlementQueryService(loader, settlements, reader);

    @BeforeEach
    void givenKirikhanShipment() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        settlements.put(FireSettlement.open(
                        TestShipments.SHIPMENT_ID, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("34032.000"))
                .afterFabricReceipt());
        settlements.givenTotals("0", "0", "32660");
    }

    private FireSettlementQuery query() {
        return new FireSettlementQuery(manager, TestShipments.SHIPMENT_ID);
    }

    private MarkInvoicedCommand invoice(String invoiceNo) {
        return new MarkInvoicedCommand(
                manager, TestShipments.SHIPMENT_ID, invoiceNo, LocalDate.parse("2026-11-12"), "ERP");
    }

    @Test
    @DisplayName("Kırıkhan: dosya yüzde 4,03 fire ve 10,720 kg hatalı kilo gösterir")
    void shouldShowKirikhanFigures() {
        // when
        FireSettlementDetails details = queryService.getFireSettlement(query());

        // then
        assertThat(details.calculation().actualFirePercent()).isEqualByComparingTo("4.03");
        assertThat(details.calculation().faultyKg().toPlainString()).isEqualTo("10.720");
    }

    @Test
    @DisplayName("Fire oranı gerekçeyle değişir, denetim kaydına yazılır ve hesap yeni orana göre yenilenir")
    void shouldChangeRateWithAuditRecord() {
        // when
        FireSettlementDetails details = rateService.changeFireRate(
                new ChangeFireRateCommand(manager, TestShipments.SHIPMENT_ID, new BigDecimal("0.05"), " Sözleşme "));

        // then
        assertThat(details.settlement().fireRate().toPlainString()).isEqualTo("0.0500");
        assertThat(details.calculation().faultyKg()).isEqualByComparingTo("0");
        assertThat(settlements.rateChanges()).singleElement().satisfies(change -> {
            assertThat(change.oldRate()).isEqualByComparingTo("0.04");
            assertThat(change.newRate()).isEqualByComparingTo("0.05");
            assertThat(change.reason()).isEqualTo("Sözleşme");
            assertThat(change.changedBy()).isEqualTo(manager.userId());
        });
        assertThat(events.names).containsExactly("fireRateChanged");
    }

    @Test
    @DisplayName("Gerekçesiz ve sınır dışı oran reddedilir")
    void shouldRejectBlankReasonAndOutOfRangeRate() {
        assertThatThrownBy(() -> rateService.changeFireRate(
                        new ChangeFireRateCommand(manager, TestShipments.SHIPMENT_ID, new BigDecimal("0.05"), " ")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> rateService.changeFireRate(
                        new ChangeFireRateCommand(manager, TestShipments.SHIPMENT_ID, new BigDecimal("0.21"), "x")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(settlements.rateChanges()).isEmpty();
    }

    @Test
    @DisplayName(
            "Hatalı kg varsa fatura edildi işaretlenir; aynı faturayla tekrar zararsızdır, farklı faturayla reddedilir")
    void shouldMarkInvoicedIdempotently() {
        // when
        FireSettlementDetails first = invoiceService.markInvoiced(invoice("F-1"));
        FireSettlementDetails repeat = invoiceService.markInvoiced(invoice("F-1"));

        // then
        assertThat(first.settlement().status()).isEqualTo(FireSettlementStatus.INVOICED);
        assertThat(first.settlement().invoicedBy()).isEqualTo(manager.userId());
        assertThat(repeat.settlement().invoiceNo()).isEqualTo("F-1");
        assertThat(events.names).containsExactly("fireInvoiced");
        assertThatThrownBy(() -> invoiceService.markInvoiced(invoice("F-2")))
                .isInstanceOf(FireSettlementClosedException.class);
    }

    @Test
    @DisplayName("Fatura edildi dosyada fire oranı değiştirilemez")
    void shouldRejectRateChangeAfterInvoice() {
        // given
        invoiceService.markInvoiced(invoice("F-1"));

        // when / then
        assertThatThrownBy(() -> rateService.changeFireRate(
                        new ChangeFireRateCommand(manager, TestShipments.SHIPMENT_ID, new BigDecimal("0.05"), "x")))
                .isInstanceOf(FireSettlementClosedException.class);
    }

    @Test
    @DisplayName("Hatalı kg varken kapatılamaz; hatalı kg sıfırsa kapanır ve tekrar kapatma zararsızdır")
    void shouldCloseOnlyWithoutFaultyKg() {
        // when / then
        assertThatThrownBy(() -> invoiceService.closeFireSettlement(query()))
                .isInstanceOf(InvalidFireSettlementActionException.class);
        settlements.givenTotals("0", "0", "32700");
        assertThat(invoiceService.closeFireSettlement(query()).settlement().status())
                .isEqualTo(FireSettlementStatus.CLOSED);
        assertThat(invoiceService.closeFireSettlement(query()).settlement().status())
                .isEqualTo(FireSettlementStatus.CLOSED);
        assertThat(events.names).containsExactly("fireSettlementClosed");
    }

    @Test
    @DisplayName("Hatalı kg sıfırken fatura edildi işaretlenemez")
    void shouldRejectInvoiceWithoutFaultyKg() {
        // given
        settlements.givenTotals("0", "0", "34032");

        // when / then
        assertThatThrownBy(() -> invoiceService.markInvoiced(invoice("F-1")))
                .isInstanceOf(InvalidFireSettlementActionException.class);
    }
}
