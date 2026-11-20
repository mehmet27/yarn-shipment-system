package com.yarnshipment.shipment.domain.fire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.FabricExceedsSentYarnException;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Parça parça faturanın ve kumaş girişi sınırının uç değerleri (KR-16): tam kalan, 1 gram eksik, 1 gram fazla. */
class FireSettlementInvoiceBoundaryTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000c01");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000c02");
    private static final UUID MANAGER = UUID.fromString("0192f0a0-0000-7000-8000-000000000c03");
    private static final LocalDate DAY = LocalDate.parse("2026-11-19");

    /** 1.000 kg gönderilmiş, 900 kg kumaş gelmiş: faturalanacak 60 kg. */
    private final FireSettlement toInvoice = FireSettlement.open(SHIPMENT, SUBCONTRACTOR, new BigDecimal("1000.000"))
            .afterFabricReceipt();

    private final FireCalculation sixtyKgShort = FireCalculation.of(new Inputs(
            new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("900"), new BigDecimal("0.04")));

    private static Invoice invoice(String invoiceNo, String kg) {
        return new Invoice(invoiceNo, DAY, null, new BigDecimal(kg));
    }

    /** Kilo yazılmayan fatura kalanın tamamını faturalar. */
    private static Invoice wholeInvoice(String invoiceNo) {
        return new Invoice(invoiceNo, DAY, null);
    }

    @Test
    @DisplayName("Kalandan 1 gram eksik fatura dosyayı açık bırakır; tam kalan dosyayı faturalandı yapar")
    void shouldStayOpenUntilExactRemainingIsInvoiced() {
        // when
        FireSettlement oneGramLeft = toInvoice.markInvoiced(sixtyKgShort, invoice("F-1", "59.999"), MANAGER);
        FireSettlement exact = toInvoice.markInvoiced(sixtyKgShort, invoice("F-1", "60.000"), MANAGER);
        FireSettlement whole = toInvoice.markInvoiced(sixtyKgShort, wholeInvoice("F-1"), MANAGER);

        // then
        assertThat(oneGramLeft.status()).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(oneGramLeft.invoiceNo()).isNull();
        assertThat(exact.status()).isEqualTo(FireSettlementStatus.INVOICED);
        assertThat(whole.status()).isEqualTo(FireSettlementStatus.INVOICED);
    }

    @Test
    @DisplayName("Kalandan 1 gram fazla, sıfır ya da eksi kilolu fatura reddedilir")
    void shouldRejectInvoiceOverRemainingOrNonPositive() {
        assertThatThrownBy(() -> toInvoice.markInvoiced(sixtyKgShort, invoice("F-1", "60.001"), MANAGER))
                .isInstanceOf(InvalidFireSettlementActionException.class)
                .hasMessageContaining("INVOICE_EXCEEDS_REMAINING");
        assertThatThrownBy(() -> toInvoice.markInvoiced(sixtyKgShort, invoice("F-1", "0"), MANAGER))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> toInvoice.markInvoiced(sixtyKgShort, invoice("F-1", "-5"), MANAGER))
                .isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("Fatura numarası boş olamaz ve en çok 64 karakterdir")
    void shouldValidateInvoiceNumberLength() {
        String longest = "F".repeat(FireSettlement.MAX_INVOICE_NO_LENGTH);

        assertThat(toInvoice
                        .markInvoiced(sixtyKgShort, wholeInvoice(longest), MANAGER)
                        .invoiceNo())
                .isEqualTo(longest);
        assertThatThrownBy(() -> toInvoice.markInvoiced(sixtyKgShort, wholeInvoice(longest + "F"), MANAGER))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> toInvoice.markInvoiced(sixtyKgShort, wholeInvoice("   "), MANAGER))
                .isInstanceOf(InvalidLedgerInputException.class);
    }

    @Test
    @DisplayName("Gelen kumaş gönderilen ipliğe tam eşit olabilir; 1 gram fazlası reddedilir")
    void shouldAllowFabricUpToSentKgExactly() {
        // given: 900 kg kumaş girilmiş
        BigDecimal received = new BigDecimal("900.000");

        // when / then
        assertThatCode(() -> toInvoice.requireFabricWithinSent(received, new BigDecimal("100.000")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> toInvoice.requireFabricWithinSent(received, new BigDecimal("100.001")))
                .isInstanceOf(FabricExceedsSentYarnException.class);
    }

    @Test
    @DisplayName("Fire oranı en çok 4 ondalık kabul eder; sınır değerler 0 ve 0,20 geçerlidir")
    void shouldValidateFireRatePrecisionAndBounds() {
        assertThat(toInvoice.withFireRate(new BigDecimal("0.0375")).fireRate().toPlainString())
                .isEqualTo("0.0375");
        assertThat(toInvoice.withFireRate(new BigDecimal("0")).fireRate().toPlainString())
                .isEqualTo("0.0000");
        assertThat(toInvoice.withFireRate(new BigDecimal("0.2")).fireRate().toPlainString())
                .isEqualTo("0.2000");
        assertThatThrownBy(() -> toInvoice.withFireRate(new BigDecimal("0.03751")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> toInvoice.withFireRate(new BigDecimal("0.20001")))
                .isInstanceOf(InvalidLedgerInputException.class);
    }
}
