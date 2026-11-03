package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.domain.model.Severity;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Fiş paneli ve "AI yanıldı" işareti; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    ComparisonAdapter.class,
    DiscrepancyWriter.class,
    DiscrepancyQueryAdapter.class,
    TicketReadingAdapter.class,
    TicketReviewAdapter.class,
    UuidV7Generator.class
})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class TicketReviewAdapterTest {

    private static final Instant MARKED_AT = Instant.parse("2026-10-24T09:00:00Z");
    private static final long MINUTE = 60L;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ComparisonAdapter comparisons;

    @Autowired
    private DiscrepancyQueryAdapter queries;

    @Autowired
    private TicketReadingAdapter readings;

    @Autowired
    private TicketReviewAdapter reviews;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Okumalar en yeniden eskiye, okunamayan dahil gelir; girilen netler karşılaştırmadan okunur")
    void shouldListReadingsAndEnteredNets() {
        SeededDiscrepancies.seed(comparisons);
        TicketReading dispatch = reading(TicketSide.DISPATCH_SCALE, true, 0);
        TicketReading unreadable = reading(TicketSide.DELIVERY_SCALE, false, 1);
        readings.saveReading(dispatch, SeededDiscrepancies.ACTOR);
        readings.saveReading(unreadable, SeededDiscrepancies.ACTOR);

        List<TicketReading> listed = reviews.listReadings(TestComparisons.SHIPMENT);

        assertThat(listed)
                .extracting(TicketReading::evidenceId)
                .containsExactly(unreadable.evidenceId(), dispatch.evidenceId());
        assertThat(listed.getFirst().read()).isFalse();
        assertThat(reviews.findEnteredNets(TestComparisons.SHIPMENT)
                        .orElseThrow()
                        .deliveryNetKg())
                .isEqualByComparingTo(TestComparisons.DISPATCH_KG);
        assertThat(reviews.findEnteredNets(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("İşaret ve geçmiş kaydı bir kez yazılır; ikinci işaret ilkini değiştirmez")
    void shouldSaveMisreadOnce() {
        SeededDiscrepancies.seed(comparisons);
        var discrepancy = SeededDiscrepancies.find(queries, Severity.RED);
        TicketReading dispatch = reading(TicketSide.DISPATCH_SCALE, true, 0);
        readings.saveReading(dispatch, SeededDiscrepancies.ACTOR);
        TicketMisread misread = new TicketMisread(
                dispatch.evidenceId(), discrepancy.id(), SeededDiscrepancies.ACTOR, MARKED_AT, "brüt yanlış");

        reviews.saveMisread(misread);
        reviews.saveMisread(new TicketMisread(
                dispatch.evidenceId(), discrepancy.id(), UUID.randomUUID(), MARKED_AT.plusSeconds(MINUTE), null));

        assertThat(reviews.findMisreads(TestComparisons.SHIPMENT)).containsExactly(misread);
        assertThat(queries.findActivity(discrepancy.id()))
                .filteredOn(activity -> "TICKET_MISREAD".equals(activity.actionType()))
                .singleElement()
                .satisfies(activity -> assertThat(activity.evidenceId()).isEqualTo(dispatch.evidenceId()));
    }

    private static TicketReading reading(TicketSide side, boolean read, long minutes) {
        return new TicketReading(
                UUID.randomUUID(),
                TestComparisons.SHIPMENT,
                side,
                read ? TestTickets.weights() : null,
                new TicketFields(TestTickets.PLATE, null, null, null),
                read ? List.of() : List.of("WEIGHT_NOT_READ"),
                TestTickets.READ_AT.plusSeconds(minutes * MINUTE));
    }
}
