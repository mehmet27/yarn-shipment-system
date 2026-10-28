package com.yarnshipment.evidence.domain.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.evidence.TestTickets;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketReadingTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");
    private static final int DAYS_LATE = 3;
    private static final EvidenceFile DISPATCH = TestTickets.file(USER, EvidenceType.DISPATCH_SCALE);

    private static TicketReading evaluate(TicketScan scan) {
        return TicketReading.evaluate(DISPATCH, scan, TestTickets.VERSION, TestUploads.NOW);
    }

    @Test
    @DisplayName("Brüt ve dara emin okunduysa okuma geçerlidir; net brüt eksi daradır")
    void shouldReadClearTicket() {
        TicketReading reading = evaluate(TestTickets.clearScan());

        assertThat(reading.status()).isEqualTo(TicketReadingStatus.READ);
        assertThat(reading.weights()).isEqualTo(new ReadWeights(TestTickets.GROSS, TestTickets.TARE));
        assertThat(Objects.requireNonNull(reading.weights()).netKg()).isEqualByComparingTo(TestTickets.NET);
        assertThat(reading.fields().plate()).isEqualTo(TestTickets.PLATE);
        assertThat(reading.problems()).isEmpty();
        assertThat(reading.evidenceType()).isEqualTo(EvidenceType.DISPATCH_SCALE);
        assertThat(reading.version()).isEqualTo(TestTickets.VERSION);
        assertThat(reading.readAt()).isEqualTo(TestUploads.NOW);
    }

    @Test
    @DisplayName("Brüt ya da dara eksik veya model emin değilse kilo okunamadı sayılır ve kilolar boş kalır")
    void shouldRequireTrustedWeights() {
        WeightValue unsure = new WeightValue(TestTickets.GROSS, ReadConfidence.MEDIUM);

        for (TicketScan scan : List.of(
                TestTickets.scan(WeightValue.MISSING, TestTickets.trusted(TestTickets.TARE), WeightValue.MISSING),
                TestTickets.scan(TestTickets.trusted(TestTickets.GROSS), WeightValue.MISSING, WeightValue.MISSING),
                TestTickets.scan(unsure, TestTickets.trusted(TestTickets.TARE), WeightValue.MISSING))) {
            TicketReading reading = evaluate(scan);
            assertThat(reading.status()).isEqualTo(TicketReadingStatus.UNREADABLE);
            assertThat(reading.weights()).isNull();
            assertThat(reading.problems()).containsExactly(ReadingProblem.WEIGHT_NOT_READ);
        }
    }

    @Test
    @DisplayName("Dara brütten küçük değilse okuma tutarsızdır")
    void shouldRejectTareNotBelowGross() {
        TicketReading reading = evaluate(TestTickets.scan(
                TestTickets.trusted(TestTickets.TARE), TestTickets.trusted(TestTickets.GROSS), WeightValue.MISSING));

        assertThat(reading.status()).isEqualTo(TicketReadingStatus.UNREADABLE);
        assertThat(reading.problems()).containsExactly(ReadingProblem.WEIGHT_INCONSISTENT);
        assertThat(evaluate(TestTickets.scan(
                                TestTickets.trusted(TestTickets.GROSS),
                                TestTickets.trusted(new BigDecimal("-1")),
                                WeightValue.MISSING))
                        .problems())
                .containsExactly(ReadingProblem.WEIGHT_INCONSISTENT);
    }

    @Test
    @DisplayName("Fişteki net brüt eksi daradan 1 kg'dan fazla saparsa alanlar karışmıştır; 1 kg'a kadar kabul edilir")
    void shouldCheckPrintedNet() {
        TicketReading mismatch = evaluate(TestTickets.scan(
                TestTickets.trusted(TestTickets.GROSS),
                TestTickets.trusted(TestTickets.TARE),
                TestTickets.trusted(new BigDecimal("4272"))));
        TicketReading withinTolerance = evaluate(TestTickets.scan(
                TestTickets.trusted(TestTickets.GROSS),
                TestTickets.trusted(TestTickets.TARE),
                TestTickets.trusted(new BigDecimal("4271"))));

        assertThat(mismatch.status()).isEqualTo(TicketReadingStatus.UNREADABLE);
        assertThat(mismatch.problems()).containsExactly(ReadingProblem.ARITHMETIC_MISMATCH);
        assertThat(withinTolerance.status()).isEqualTo(TicketReadingStatus.READ);
        assertThat(evaluate(TestTickets.scan(
                                TestTickets.trusted(TestTickets.GROSS),
                                TestTickets.trusted(TestTickets.TARE),
                                WeightValue.MISSING))
                        .status())
                .isEqualTo(TicketReadingStatus.READ);
    }

    @Test
    @DisplayName(
            "Fiş tarihi 48 saatten fazla saparsa uyarı yazılır ama okuma geçerli kalır; fotoğraf sorunları eklenir")
    void shouldWarnWithoutBlocking() {
        TicketScan scan = TestTickets.scanAt(
                TestUploads.NOW.minus(Duration.ofDays(DAYS_LATE)), List.of(ReadingProblem.GLARE, ReadingProblem.GLARE));

        TicketReading reading = evaluate(scan);

        assertThat(reading.status()).isEqualTo(TicketReadingStatus.READ);
        assertThat(reading.problems()).containsExactly(ReadingProblem.DATE_OUT_OF_RANGE, ReadingProblem.GLARE);
        assertThat(evaluate(TestTickets.scanAt(TestUploads.NOW.minus(Duration.ofDays(1)), List.of()))
                        .problems())
                .isEmpty();
    }

    @Test
    @DisplayName("Okunamayan görüntü kilo okunamadı ve biçim sorunuyla döner")
    void shouldMarkUnsupportedImage() {
        TicketReading reading = evaluate(TicketScan.unreadableImage(ReadingProblem.UNSUPPORTED_FORMAT));

        assertThat(reading.status()).isEqualTo(TicketReadingStatus.UNREADABLE);
        assertThat(reading.problems())
                .containsExactly(ReadingProblem.WEIGHT_NOT_READ, ReadingProblem.UNSUPPORTED_FORMAT);
        assertThat(reading.fields()).isEqualTo(TicketFields.EMPTY);
    }

    @Test
    @DisplayName("Yalnızca kantar fişleri okunur; teslim fişi de okunabilir")
    void shouldReadOnlyScaleTickets() {
        EvidenceFile seal = TestTickets.file(USER, EvidenceType.SEAL);

        assertThatThrownBy(() ->
                        TicketReading.evaluate(seal, TestTickets.clearScan(), TestTickets.VERSION, TestUploads.NOW))
                .isInstanceOf(TicketReadingNotSupportedException.class);
        assertThat(TicketReading.isReadable(EvidenceType.DELIVERY_SCALE)).isTrue();
        assertThat(TicketReading.evaluate(
                                TestTickets.file(USER, EvidenceType.DELIVERY_SCALE),
                                TestTickets.clearScan(),
                                TestTickets.VERSION,
                                TestUploads.NOW)
                        .evidenceType())
                .isEqualTo(EvidenceType.DELIVERY_SCALE);
    }

    @Test
    @DisplayName("Kilolar yalnızca okunmuş kayıtta bulunur; yalnızca kilo sorunları yeniden çekim gerektirir")
    void shouldKeepInvariants() {
        TicketReading read = evaluate(TestTickets.clearScan());

        assertThatThrownBy(() -> unreadableWithWeights(read)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ReadingProblem.ARITHMETIC_MISMATCH.blocksReading()).isTrue();
        assertThat(ReadingProblem.BLURRY.blocksReading()).isFalse();
        assertThat(new WeightValue(null, ReadConfidence.HIGH).isTrusted()).isFalse();
    }

    private static TicketReading unreadableWithWeights(TicketReading read) {
        return new TicketReading(
                read.evidenceId(),
                read.shipmentId(),
                read.evidenceType(),
                TicketReadingStatus.UNREADABLE,
                read.weights(),
                read.fields(),
                List.of(),
                read.version(),
                read.rawResponse(),
                read.readAt());
    }

    @Test
    @DisplayName("Okuma kullanılamıyor hatası nedeniyle birlikte taşınır")
    void shouldCarryCause() {
        IllegalStateException cause = new IllegalStateException("down");

        assertThat(new TicketReaderUnavailableException("failed", cause)).hasCause(cause);
    }
}
