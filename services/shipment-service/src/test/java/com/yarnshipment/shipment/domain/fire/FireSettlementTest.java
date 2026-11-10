package com.yarnshipment.shipment.domain.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FireSettlementTest {

    private static final UUID SHIPMENT = UUID.randomUUID();
    private static final UUID MANAGER = UUID.randomUUID();
    private static final Invoice INVOICE = new Invoice(" F-2026-1 ", LocalDate.parse("2026-11-12"), "not");

    private final FireSettlement opened = FireSettlement.open(SHIPMENT, UUID.randomUUID(), new BigDecimal("1000.000"));

    private static FireCalculation fabric(FireSettlement settlement, String fabricKg) {
        return FireCalculation.of(new Inputs(
                settlement.sentKg(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(fabricKg),
                settlement.fireRate()));
    }

    @Test
    @DisplayName("Açılan dosya varsayılan yüzde 4 oranla kumaş bekler")
    void shouldOpenWithDefaultRate() {
        assertThat(opened.status()).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
        assertThat(opened.fireRate()).isEqualByComparingTo("0.0400");
    }

    @Test
    @DisplayName("İlk kumaş girişinden sonra dosya faturalanmaya hazırdır")
    void shouldMoveToInvoiceAfterFabricReceipt() {
        assertThat(opened.afterFabricReceipt().status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
    }

    @Test
    @DisplayName("Hatalı kg varsa fatura edildi işaretlenir, fatura numarası kırpılır")
    void shouldMarkInvoicedWhenFaultyKg() {
        // given
        FireSettlement toInvoice = opened.afterFabricReceipt();

        // when
        FireSettlement invoiced = toInvoice.markInvoiced(fabric(toInvoice, "900"), INVOICE, MANAGER);

        // then
        assertThat(invoiced.status()).isEqualTo(FireSettlementStatus.INVOICED);
        assertThat(invoiced.invoiceNo()).isEqualTo("F-2026-1");
        assertThat(invoiced.invoicedBy()).isEqualTo(MANAGER);
        assertThat(invoiced.invoicedOn()).isEqualTo(LocalDate.parse("2026-11-12"));
    }

    @Test
    @DisplayName("Hatalı kg sıfırsa fatura edildi işaretlenemez")
    void shouldRejectInvoiceWithoutFaultyKg() {
        // given
        FireSettlement toInvoice = opened.afterFabricReceipt();

        // when / then
        assertThatThrownBy(() -> toInvoice.markInvoiced(fabric(toInvoice, "960"), INVOICE, MANAGER))
                .isInstanceOf(InvalidFireSettlementActionException.class);
    }

    @Test
    @DisplayName("Kumaş girmemiş dosya faturalanamaz")
    void shouldRejectInvoiceBeforeFabric() {
        assertThatThrownBy(() -> opened.markInvoiced(fabric(opened, "0"), INVOICE, MANAGER))
                .isInstanceOf(InvalidFireSettlementActionException.class);
    }

    @Test
    @DisplayName("Hatalı kg sıfırsa dosya kapatılır, hatalı kg varsa kapatılamaz")
    void shouldCloseOnlyWithoutFaultyKg() {
        // given
        FireSettlement toInvoice = opened.afterFabricReceipt();

        // when / then
        assertThat(toInvoice.close(fabric(toInvoice, "960")).status()).isEqualTo(FireSettlementStatus.CLOSED);
        assertThatThrownBy(() -> toInvoice.close(fabric(toInvoice, "900")))
                .isInstanceOf(InvalidFireSettlementActionException.class);
    }

    @Test
    @DisplayName("Kapanmış dosyada kumaş girişi, oran değişikliği ve ikinci fatura işareti reddedilir")
    void shouldRejectChangesOnClosedSettlement() {
        // given
        FireSettlement toInvoice = opened.afterFabricReceipt();
        FireSettlement invoiced = toInvoice.markInvoiced(fabric(toInvoice, "900"), INVOICE, MANAGER);
        FireSettlement closed = toInvoice.close(fabric(toInvoice, "960"));

        // when / then
        for (FireSettlement settlement : new FireSettlement[] {invoiced, closed}) {
            assertThatThrownBy(settlement::afterFabricReceipt).isInstanceOf(FireSettlementClosedException.class);
            assertThatThrownBy(() -> settlement.withFireRate(new BigDecimal("0.0500")))
                    .isInstanceOf(FireSettlementClosedException.class);
            assertThatThrownBy(() -> settlement.markInvoiced(fabric(settlement, "900"), INVOICE, MANAGER))
                    .isInstanceOf(FireSettlementClosedException.class);
        }
    }

    @Test
    @DisplayName("Fire oranı 0 ile 0,20 arasında kabul edilir, dışı reddedilir")
    void shouldValidateFireRateRange() {
        assertThat(opened.withFireRate(new BigDecimal("0.05")).fireRate().toPlainString())
                .isEqualTo("0.0500");
        assertThat(opened.withFireRate(new BigDecimal("0.2000")).fireRate()).isEqualByComparingTo("0.2");
        assertThat(opened.withFireRate(BigDecimal.ZERO).fireRate()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> opened.withFireRate(new BigDecimal("0.2001")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> opened.withFireRate(new BigDecimal("-0.01")))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
