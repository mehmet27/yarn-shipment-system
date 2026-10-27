package com.yarnshipment.discrepancy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketReuse;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import com.yarnshipment.discrepancy.domain.ticket.WeighingEntries;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketChecksTest {

    private static final ToleranceBands BANDS = ToleranceBands.REPORT_DEFAULT;

    @Test
    @DisplayName("Girişler fişlerle 1 kg içinde, plakalar boşluk ve tire yok sayılarak tutuyorsa bulgu yok")
    void shouldPassWhenTicketsMatchEntries() {
        ShipmentTickets withinOneKg =
                TestTickets.both(TestTickets.weights("2101.000", "99.500"), TestTickets.weights());

        assertThat(TicketChecks.compare(TestTickets.entries(), TestTickets.matching(), BANDS))
                .isEmpty();
        assertThat(TicketChecks.compare(TestTickets.entries(), withinOneKg, BANDS))
                .isEmpty();
    }

    @Test
    @DisplayName("Okunmuş fiş ya da çıkış tartımı yoksa fiş kontrolleri atlanır")
    void shouldSkipWithoutTickets() {
        WeighingEntries noDispatch = new WeighingEntries(null, TestTickets.weights(), null);
        ShipmentTickets unreadable =
                new ShipmentTickets(TestTickets.reading(TicketSide.DISPATCH_SCALE, null), null, List.of());

        assertThat(TicketChecks.compare(TestTickets.entries(), ShipmentTickets.NONE, BANDS))
                .isEmpty();
        assertThat(TicketChecks.compare(TestTickets.entries(), unreadable, BANDS))
                .isEmpty();
        assertThat(TicketChecks.compare(noDispatch, TestTickets.matching(), BANDS))
                .isEmpty();
    }

    @Test
    @DisplayName("K1: depo girişi çıkış fişinden farklıysa belge uyuşmazlığı; öncelik net farkın bandından")
    void shouldFlagDispatchEntryMismatch() {
        ShipmentTickets darker = TestTickets.both(TestTickets.weights("2100.000", "102.000"), TestTickets.weights());
        ShipmentTickets other = TestTickets.both(TestTickets.weights("2200.000", "100.000"), TestTickets.weights());

        Finding yellow = only(TicketChecks.compare(TestTickets.entries(), darker, BANDS));
        Finding red = TicketChecks.compare(TestTickets.entries(), other, BANDS).getFirst();

        assertThat(yellow.ruleCode()).isEqualTo(TicketChecks.DISPATCH_WEIGHT_MISMATCH);
        assertThat(yellow.type()).isEqualTo(DiscrepancyType.DOCUMENT);
        assertThat(yellow.severity()).isEqualTo(Severity.YELLOW);
        assertThat(yellow.expectedValue()).isEqualTo("gross=2100.000 tare=100.000 net=2000.000");
        assertThat(yellow.actualValue()).isEqualTo("gross=2100.000 tare=102.000 net=1998.000");
        assertThat(yellow.diffKg()).isEqualByComparingTo("2");
        assertThat(yellow.diffPct()).isEqualByComparingTo("0.10");
        assertThat(red.ruleCode()).isEqualTo(TicketChecks.DISPATCH_WEIGHT_MISMATCH);
        assertThat(red.severity()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("K2: fasoncu girişi teslim fişinden farklıysa belge uyuşmazlığı")
    void shouldFlagDeliveryEntryMismatch() {
        WeighingEntries entries =
                new WeighingEntries(null, TestTickets.weights("2100.000", "110.000"), TestTickets.PLATE);
        ShipmentTickets tickets = new ShipmentTickets(
                null, TestTickets.reading(TicketSide.DELIVERY_SCALE, TestTickets.weights()), List.of());

        Finding finding = only(TicketChecks.compare(entries, tickets, BANDS));

        assertThat(finding.ruleCode()).isEqualTo(TicketChecks.DELIVERY_WEIGHT_MISMATCH);
        assertThat(finding.severity()).isEqualTo(Severity.YELLOW);
        assertThat(finding.diffKg()).isEqualByComparingTo("-10");
    }

    @Test
    @DisplayName("K3: iki fişin neti tolerans dışındaysa elle karşılaştırmayla aynı bantta miktar uyuşmazlığı")
    void shouldCompareTicketNets() {
        ShipmentTickets yellowGap = TestTickets.both(TestTickets.weights(), TestTickets.weights("2080.000", "100.000"));
        ShipmentTickets redGap = TestTickets.both(TestTickets.weights(), TestTickets.weights("2050.000", "100.000"));
        WeighingEntries noEntries = new WeighingEntries(null, TestTickets.weights("2080.000", "100.000"), null);

        Finding yellow = only(TicketChecks.compare(noEntries, yellowGap, BANDS));
        Finding red = only(TicketChecks.compare(
                new WeighingEntries(null, TestTickets.weights("2050.000", "100.000"), null), redGap, BANDS));

        assertThat(yellow.ruleCode()).isEqualTo(TicketChecks.QUANTITY_TOLERANCE);
        assertThat(yellow.type()).isEqualTo(DiscrepancyType.QUANTITY);
        assertThat(yellow.severity()).isEqualTo(Severity.YELLOW);
        assertThat(yellow.expectedValue()).isEqualTo("2000.000");
        assertThat(yellow.actualValue()).isEqualTo("1980.000");
        assertThat(yellow.diffPct()).isEqualByComparingTo("1.00");
        assertThat(red.severity()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("K4: fişteki plaka sevkiyatın plakasından farklıysa her fiş için sarı belge uyuşmazlığı")
    void shouldFlagPlateMismatch() {
        WeighingEntries otherPlate = new WeighingEntries(TestTickets.weights(), TestTickets.weights(), "06 XYZ 99");
        WeighingEntries noPlate = new WeighingEntries(TestTickets.weights(), TestTickets.weights(), " ");

        List<Finding> findings = TicketChecks.compare(otherPlate, TestTickets.matching(), BANDS);

        assertThat(findings)
                .extracting(Finding::ruleCode)
                .containsExactly(
                        ComparisonRule.TICKET_DISPATCH_PLATE_MISMATCH.name(),
                        ComparisonRule.TICKET_DELIVERY_PLATE_MISMATCH.name());
        assertThat(findings.getFirst().severity()).isEqualTo(Severity.YELLOW);
        assertThat(findings.getFirst().expectedValue()).isEqualTo("06 XYZ 99");
        assertThat(findings.getFirst().actualValue()).isEqualTo(" 34 abc-123 ");
        assertThat(TicketChecks.compare(noPlate, TestTickets.matching(), BANDS)).isEmpty();
    }

    @Test
    @DisplayName("K5: fiş başka sevkiyatta da okunmuşsa kırmızı belge uyuşmazlığı")
    void shouldFlagReusedTicket() {
        UUID other = UUID.randomUUID();
        ShipmentTickets matching = TestTickets.matching();
        ShipmentTickets reused = new ShipmentTickets(
                matching.dispatch(),
                matching.delivery(),
                List.of(
                        new TicketReuse(TicketSide.DISPATCH_SCALE, TestTickets.TICKET_NO, other),
                        new TicketReuse(TicketSide.DELIVERY_SCALE, TestTickets.TICKET_NO, other)));

        List<Finding> findings = TicketChecks.compare(TestTickets.entries(), reused, BANDS);

        assertThat(findings)
                .extracting(Finding::ruleCode)
                .containsExactly(
                        ComparisonRule.TICKET_DISPATCH_REUSED.name(), ComparisonRule.TICKET_DELIVERY_REUSED.name());
        assertThat(findings.getFirst().severity()).isEqualTo(Severity.RED);
        assertThat(findings.getFirst().expectedValue()).isEqualTo(other.toString());
        assertThat(findings.getFirst().actualValue()).isEqualTo(TestTickets.TICKET_NO);
    }

    @Test
    @DisplayName("Fiş bulguları elle karşılaştırmanın sonucuna eklenir; miktar bandı değişmez")
    void shouldAppendToComparisonResult() {
        ComparisonResult base = Comparison.compare(TestComparisons.clean(), BANDS);
        List<Finding> extra = TicketChecks.compare(
                TestTickets.entries(),
                TestTickets.both(TestTickets.weights(), TestTickets.weights("2050.000", "100.000")),
                BANDS);

        ComparisonResult result = base.plus(extra);

        assertThat(result.clean()).isFalse();
        assertThat(result.quantityBand()).isEqualTo(Band.GREEN);
        assertThat(result.findings()).hasSize(extra.size());
    }

    private static Finding only(List<Finding> findings) {
        assertThat(findings).hasSize(1);
        return findings.getFirst();
    }
}
