package com.yarnshipment.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.app.Factory.Party;
import com.yarnshipment.app.Factory.Yarn;
import com.yarnshipment.app.FactoryHttp.Reply;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.moduleapi.event.DomainEvent;
import com.yarnshipment.moduleapi.event.DomainEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Parça parça faturalama ve haklı kalite itirazının sayıları (analiz 5.11, 5.12, 8.1). Beklenen değerler elle
 * hesaplanmıştır: bakiye = G - D - K - faturalanan eksik kilo; itirazı karşılayan fatura bakiyeyi ikinci kez düşürmez.
 */
@FactoryApplicationTest
class InvoicingAndClaimNumbersTest {

    private static final String JUSTIFIED = "CLAIM_JUSTIFIED";
    private static final int INVOICE_COUNT = 3;

    private final Factory factory;
    private final DomainEventPublisher events;
    private final TransactionTemplate transaction;

    @Autowired
    InvoicingAndClaimNumbersTest(
            Factory factory, DomainEventPublisher events, PlatformTransactionManager transactionManager) {
        this.factory = factory;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Eksik kumaş üç faturada kapanır: kalan kilo ve bakiye her faturada tam fatura kilosu kadar düşer")
    void shouldDeductExactlyInvoicedKgWhenShortageIsInvoicedPiecewise() {
        // given: G = P 600 + Q 400 = 1.000 kg, fire %4 -> B = 960; kumaş 900 (P 540, Q 360) -> H = 60
        Party party = factory.newSubcontractor("Boyahane");
        Yarn viskon = factory.newYarn("36/1 Viskon");
        Yarn poly = factory.newYarn("75 Denye Poly");
        UUID shipmentId = factory.sendYarn(party, "0.0400", List.of(viskon.kg("600.000"), poly.kg("400.000")));
        factory.receiveFabric(shipmentId, "900.000", Map.of()).ok();
        assertThat(factory.balanceOf(party))
                .containsEntry(viskon.name(), "60.000")
                .containsEntry(poly.name(), "40.000");

        // when: ilk fatura yalnız P ipliğinden 20 kg; aynı fatura numarası bir kez daha gönderilir
        String firstNo = FactoryHttp.unique("FTR");
        factory.invoice(shipmentId, firstNo, Map.of(viskon.id().toString(), "20.000"))
                .ok();
        JsonNode repeated = factory.invoice(
                        shipmentId, firstNo, Map.of(viskon.id().toString(), "20.000"))
                .ok()
                .body();

        // then: kalan 60 - 20 = 40; P 60 - 20 = 40; tekrar ikinci kez düşmez; fire = (1000 - 900 - 20) / 1000 = %8
        assertInvoicing(repeated, "TO_INVOICE", "20.000", "40.000", "8.00");
        assertThat(factory.balanceOf(party))
                .containsEntry(viskon.name(), "40.000")
                .containsEntry(poly.name(), "40.000")
                .containsEntry("TOPLAM", "80.000");
        assertThat(factory.toInvoiceRow(shipmentId))
                .hasValueSatisfying(
                        row -> assertThat(row.required("shortageKg").asString()).isEqualTo("40.000"));

        // when: ikinci fatura Q 15,5 + P 4,5 = 20 kg
        Map<String, String> second =
                Map.of(poly.id().toString(), "15.500", viskon.id().toString(), "4.500");
        JsonNode afterSecond = factory.invoice(shipmentId, FactoryHttp.unique("FTR"), second)
                .ok()
                .body();

        // then: kalan 20; P 35,5, Q 24,5; fire = (1000 - 900 - 40) / 1000 = %6
        assertInvoicing(afterSecond, "TO_INVOICE", "40.000", "20.000", "6.00");
        assertThat(factory.balanceOf(party))
                .containsEntry(viskon.name(), "35.500")
                .containsEntry(poly.name(), "24.500")
                .containsEntry("TOPLAM", "60.000");

        assertOverInvoicingAndExcessFabricRejected(shipmentId, viskon);

        // when: kalan, satır yazılmadan faturalanır; 20 kg gönderilen iplik oranıyla (600:400) P 12 + Q 8 dağılır
        JsonNode closed = factory.invoice(shipmentId, FactoryHttp.unique("FTR"), Map.of())
                .ok()
                .body();

        // then: dosya ancak şimdi kapanır; bakiye 1000 - 900 - 60 = 40 (P 35,5 - 12, Q 24,5 - 8)
        assertInvoicedReadSides(party, closed, Map.of(viskon.name(), "23.500", poly.name(), "16.500"));
    }

    /** Kalandan fazla fatura ve gönderilen ipliği aşan kumaş reddedilir; fire dosyasında hiçbir sayı değişmez. */
    private void assertOverInvoicingAndExcessFabricRejected(UUID shipmentId, Yarn viskon) {
        // when: kalandan fazla fatura ve gönderilen ipliği aşan kumaş reddedilir (900 + 40 + 60,001 > 1000)
        Reply tooMuch = factory.invoice(
                shipmentId, FactoryHttp.unique("FTR"), Map.of(viskon.id().toString(), "20.001"));
        Reply tooMuchFabric = factory.receiveFabric(shipmentId, "60.001", Map.of());

        // then: hiçbir sayı değişmez
        assertThat(tooMuch.status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(tooMuch.text("reason")).isEqualTo("INVOICE_EXCEEDS_REMAINING");
        assertThat(tooMuchFabric.status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(tooMuchFabric.text("code")).isEqualTo("FABRIC_EXCEEDS_SENT_YARN");
        assertInvoicing(factory.fireFile(shipmentId), "TO_INVOICE", "40.000", "20.000", "6.00");
    }

    @Test
    @DisplayName("Haklı itiraz faturalanacak kiloya eklenir, bakiyeyi ikinci kez düşürmez; haksız itiraz etkisizdir")
    void shouldAddUpheldClaimToInvoiceableKgWithoutDoubleDeduction() {
        // given: G = 1.000 kg, fire %4; kumaş 964 -> bakiye 36; B = 960, E = 960 - 964 = -4 -> H = 0; fire %3,60
        Party party = factory.newSubcontractor("Dokuma");
        Yarn yarn = factory.newYarn("30/1 Karde");
        UUID shipmentId = factory.sendYarn(party, "0.0400", List.of(yarn.kg("1000.000")));
        factory.receiveFabric(shipmentId, "964.000", Map.of()).ok();
        Factory.assertFireFile(factory.fireFile(shipmentId), "TO_INVOICE", "1000.000", "960.000", "0.000", "3.60");

        // when: 50 kg'lık itiraz haksız bulunur
        factory.decideClaim(factory.openQualityClaim(shipmentId, "50.000"), "CLAIM_UNJUSTIFIED")
                .ok();

        // then: hiçbir sayı değişmez, faturalanacak bir şey yoktur
        Factory.assertFireFile(factory.fireFile(shipmentId), "TO_INVOICE", "1000.000", "960.000", "0.000", "3.60");
        assertThat(factory.toInvoiceRow(shipmentId)).isEmpty();
        assertThat(factory.balanceOf(party)).containsEntry("TOPLAM", "36.000");

        // when: 100 kg'lık itiraz haklı bulunur
        UUID upheld = factory.openQualityClaim(shipmentId, "100.000");
        factory.decideClaim(upheld, JUSTIFIED).ok();

        // then: I = 100 -> T = 900, B = 864, E = 864 - (964 - 100) = 0, fire = (900 - 864) / 900 = %4,00
        // itiraz kilosu faturalanacak kilodur; bakiye fatura kesilene kadar da kesildikten sonra da 36 kalır
        JsonNode withClaim = factory.fireFile(shipmentId);
        Factory.assertFireFile(withClaim, "TO_INVOICE", "900.000", "864.000", "0.000", "4.00");
        assertThat(withClaim.required("claimKg").asString()).isEqualTo("100.000");
        assertThat(withClaim.required("remainingKg").asString()).isEqualTo("100.000");
        assertThat(factory.balanceOf(party)).containsEntry("TOPLAM", "36.000");
        assertThat(factory.toInvoiceRow(shipmentId))
                .hasValueSatisfying(
                        row -> assertThat(row.required("shortageKg").asString()).isEqualTo("100.000"));
        assertThat(factory.fireReportRow(party, shipmentId)
                        .required("actualFireRate")
                        .asString())
                .isEqualTo("0.0400");

        // when: müdür itirazın faturasını girer
        JsonNode invoiced = factory.invoice(shipmentId, FactoryHttp.unique("FTR"), Map.of())
                .ok()
                .body();

        // then: dosya kapanır; faturanın tamamı itirazı karşılar, eksik kumaş için düşülen kilo yoktur
        assertInvoicing(invoiced, "INVOICED", "100.000", "0.000", "4.00");
        assertThat(invoiced.required("shortageInvoicedKg").asString()).isEqualTo("0.000");
        assertThat(factory.balanceOf(party))
                .containsEntry(yarn.name(), "36.000")
                .containsEntry("TOPLAM", "36.000");
        assertThat(factory.invoicedList(party).required("totalKg").asString()).isEqualTo("100.000");

        // when: aynı karar olayı ikinci kez teslim edilir
        redeliverDecision(upheld, shipmentId, "100.000");

        // then: kapanmış dosya yeniden açılmaz, sayılar aynı kalır
        assertInvoicing(factory.fireFile(shipmentId), "INVOICED", "100.000", "0.000", "4.00");
        assertThat(factory.fireFile(shipmentId).required("claimKg").asString()).isEqualTo("100.000");
    }

    @Test
    @DisplayName("Faturalanmış dosyaya gelen haklı itiraz dosyayı yeniden açar; yeni fatura yalnız eksik kiloyu düşer")
    void shouldReopenInvoicedFileWhenLaterClaimIsUpheld() {
        // given: G = 1.000, kumaş 964, 100 kg haklı itiraz faturalanmış; dosya kapalı, bakiye 36
        Party party = factory.newSubcontractor("Dokuma");
        Yarn yarn = factory.newYarn("30/1 Karde");
        UUID shipmentId = factory.sendYarn(party, "0.0400", List.of(yarn.kg("1000.000")));
        factory.receiveFabric(shipmentId, "964.000", Map.of()).ok();
        factory.decideClaim(factory.openQualityClaim(shipmentId, "100.000"), JUSTIFIED)
                .ok();
        factory.invoice(shipmentId, FactoryHttp.unique("FTR"), Map.of()).ok();

        // when: 30 kg'lık yeni itiraz haklı bulunur
        factory.decideClaim(factory.openQualityClaim(shipmentId, "30.000"), JUSTIFIED)
                .ok();

        // then: I = 130 -> T = 870, B = 870 x 0,96 = 835,2; E = 835,2 - (964 - 130) = 1,2 -> H = 1,2
        // kalan = 1,2 (eksik) + 30 (itiraz) = 31,2; fire = (870 - 834) / 870 = %4,14
        JsonNode reopened = factory.fireFile(shipmentId);
        Factory.assertFireFile(reopened, "TO_INVOICE", "870.000", "835.200", "1.200", "4.14");
        assertThat(reopened.required("remainingKg").asString()).isEqualTo("31.200");
        assertThat(reopened.required("claimRemainingKg").asString()).isEqualTo("30.000");
        assertThat(factory.toInvoiceRow(shipmentId))
                .hasValueSatisfying(
                        row -> assertThat(row.required("shortageKg").asString()).isEqualTo("31.200"));

        // when: kalan 31,2 kg faturalanır
        JsonNode closed = factory.invoice(shipmentId, FactoryHttp.unique("FTR"), Map.of())
                .ok()
                .body();

        // then: 30 kg itirazı karşılar (bakiye değişmez), 1,2 kg eksik kumaş bakiyeden düşer: 36 - 1,2 = 34,8
        // bakiye = G - K - faturalanan eksik = 1000 - 964 - 1,2; fire = (870 - 834 - 1,2) / 870 = %4,00
        assertInvoicing(closed, "INVOICED", "131.200", "0.000", "4.00");
        assertThat(closed.required("shortageInvoicedKg").asString()).isEqualTo("1.200");
        assertThat(factory.balanceOf(party)).containsEntry("TOPLAM", "34.800");
        assertThat(factory.matrixOf(party)).containsEntry(yarn.name(), "34.800");
        assertThat(factory.invoicedList(party).required("totalKg").asString()).isEqualTo("131.200");
    }

    /**
     * Son faturadan sonra: H = 60 kg'ın tamamı faturalanmıştır, fire = (1000 - 900 - 60) / 1000 = %4. Faturalananlar
     * listesi üç faturayı 20 + 20 + 20 = 60 kg gösterir; dosya artık Faturalanacaklar'da değildir.
     */
    private void assertInvoicedReadSides(Party party, JsonNode closed, Map<String, String> balanceByYarn) {
        UUID shipmentId = UUID.fromString(closed.required("shipmentId").asString());
        assertInvoicing(closed, "INVOICED", "60.000", "0.000", "4.00");
        assertThat(closed.required("faultyKg").asString()).isEqualTo("60.000");
        assertThat(factory.balanceOf(party)).containsAllEntriesOf(balanceByYarn).containsEntry("TOPLAM", "40.000");
        JsonNode invoiced = factory.invoicedList(party);
        assertThat(invoiced.required("items")).hasSize(INVOICE_COUNT);
        assertThat(invoiced.required("totalKg").asString()).isEqualTo("60.000");
        assertThat(factory.toInvoiceRow(shipmentId)).isEmpty();
        JsonNode report = factory.fireReportRow(party, shipmentId);
        assertThat(report.required("status").asString()).isEqualTo("CLOSED");
        assertThat(report.required("shortageKg").asString()).isEqualTo("60.000");
        assertThat(report.required("actualFireRate").asString()).isEqualTo("0.0400");
        assertThat(factory.matrixOf(party).values().stream()
                        .map(BigDecimal::new)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("40.000");
    }

    /** Sırasıyla dosya durumu, faturalanan toplam, kalan faturalanacak kilo ve gerçekleşen fire yüzdesi. */
    private static void assertInvoicing(JsonNode file, String status, String... figures) {
        assertThat(file.required("status").asString()).isEqualTo(status);
        assertThat(List.of(
                        file.required("invoicedKg").asString(),
                        file.required("remainingKg").asString(),
                        file.required("actualFirePercent").asString()))
                .containsExactly(figures);
    }

    private void redeliverDecision(UUID discrepancyId, UUID shipmentId, String claimedKg) {
        Instant decidedAt = Instant.parse("2026-11-20T09:00:00Z");
        DiscrepancyResolved payload = new DiscrepancyResolved(
                discrepancyId,
                "UYS-TEKRAR",
                shipmentId,
                UUID.randomUUID(),
                JUSTIFIED,
                decidedAt,
                0,
                new BigDecimal(claimedKg));
        DomainEvent event = new DomainEvent(
                "Discrepancy", discrepancyId, DiscrepancyResolved.TYPE, payload, decidedAt, UUID.randomUUID());
        transaction.executeWithoutResult(status -> events.publish(event));
    }
}
