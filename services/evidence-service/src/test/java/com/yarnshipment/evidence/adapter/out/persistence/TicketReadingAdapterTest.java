package com.yarnshipment.evidence.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.evidence.TicketRead;
import com.yarnshipment.evidence.TestTickets;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketReadingStatus;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Fiş okuması; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EvidenceStoreAdapter.class, EvidenceOutbox.class, TicketReadingAdapter.class, UuidV7Generator.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class TicketReadingAdapterTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a2");

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private EvidenceStoreAdapter evidence;

    @Autowired
    private TicketReadingAdapter readings;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Okunan fiş kilolar ve alanlarla yazılıp okunur; olmayan kimlik boş döner")
    void shouldRoundTripReadTicket() {
        TicketReading reading =
                TicketReading.evaluate(savedEvidence(), TestTickets.clearScan(), TestTickets.VERSION, TestUploads.NOW);

        readings.saveReading(reading);

        TicketReading found = readings.findReading(reading.evidenceId()).orElseThrow();
        assertThat(found.status()).isEqualTo(TicketReadingStatus.READ);
        assertThat(Objects.requireNonNull(found.weights()).grossKg()).isEqualByComparingTo(TestTickets.GROSS);
        assertThat(Objects.requireNonNull(found.weights()).netKg()).isEqualByComparingTo(TestTickets.NET);
        assertThat(found.fields()).isEqualTo(reading.fields());
        assertThat(found.version()).isEqualTo(TestTickets.VERSION);
        assertThat(found.readAt()).isEqualTo(TestUploads.NOW);
        assertThat(found.problems()).isEmpty();
        assertThat(readings.findReading(UUID.randomUUID())).isEmpty();
        assertThat(eventGrossKg(reading.shipmentId())).isEqualTo(TestTickets.GROSS.toPlainString());
    }

    @Test
    @DisplayName("Okunamayan fiş sorunlarıyla yazılır; aynı kanıta ikinci okuma ilkini değiştirmez")
    void shouldKeepFirstReading() {
        EvidenceFile file = savedEvidence();
        TicketReading unreadable = TicketReading.evaluate(
                file,
                TicketScan.unreadableImage(ReadingProblem.UNSUPPORTED_FORMAT),
                TestTickets.VERSION,
                TestUploads.NOW);

        readings.saveReading(unreadable);
        readings.saveReading(
                TicketReading.evaluate(file, TestTickets.clearScan(), TestTickets.VERSION, TestUploads.NOW));

        TicketReading found = readings.findReading(file.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(TicketReadingStatus.UNREADABLE);
        assertThat(found.weights()).isNull();
        assertThat(found.problems()).containsExactly(ReadingProblem.WEIGHT_NOT_READ, ReadingProblem.UNSUPPORTED_FORMAT);
        assertThat(eventCount(file.shipmentId())).isEqualTo(1);
        assertThat(eventGrossKg(file.shipmentId())).isNull();
    }

    private int eventCount(UUID shipmentId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                Integer.class,
                shipmentId,
                TicketRead.TYPE);
        return count == null ? 0 : count;
    }

    /** Olaydaki brüt; okunamayan fişte {@code weights} boş olduğu için {@code null}. */
    private @Nullable String eventGrossKg(UUID shipmentId) {
        return jdbc.queryForObject(
                "SELECT payload -> 'weights' ->> 'grossKg' FROM outbox_event WHERE aggregate_id = ? AND event_type = ?",
                String.class,
                shipmentId,
                TicketRead.TYPE);
    }

    private EvidenceFile savedEvidence() {
        UUID id = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        UploadIntent intent = new UploadIntent(
                id,
                shipmentId,
                TestTokens.SITE_ID,
                TestUploads.declared(EvidenceType.DISPATCH_SCALE),
                UploadIntent.storageKeyOf(TestUploads.NOW, shipmentId, id),
                USER,
                TestUploads.NOW,
                TestUploads.expiry());
        EvidenceFile file = EvidenceFile.confirm(intent, TestUploads.stored(), TestUploads.NOW);
        evidence.saveEvidence(file);
        return file;
    }
}
