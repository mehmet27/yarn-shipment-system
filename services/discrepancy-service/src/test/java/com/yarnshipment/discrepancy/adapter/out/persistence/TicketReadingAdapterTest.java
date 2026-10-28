package com.yarnshipment.discrepancy.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.domain.ticket.ScaleWeights;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketFields;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketReuse;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Kantar fişi okumaları; migration'larla kurulan gerçek şemada ve uygulama rolüyle. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TicketReadingAdapter.class)
class TicketReadingAdapterTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d002");
    private static final long MINUTE = 60L;
    private static final int TICKET_LENGTH = 8;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private TicketReadingAdapter readings;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Her taraf için en son okunan fiş seçilir; okunamayan ve tekrar gelen okuma onu değiştirmez")
    void shouldPickLatestReadTicketPerSide() {
        UUID shipment = UUID.randomUUID();
        TicketReading first = reading(shipment, TestTickets.weights(), fields(uniqueTicket()), 0);
        TicketReading retake = reading(shipment, TestTickets.weights("2100.000", "99.000"), fields(uniqueTicket()), 1);
        TicketReading unreadable = reading(shipment, null, fields(uniqueTicket()), 2);
        TicketReading repeated = new TicketReading(
                retake.evidenceId(),
                shipment,
                TicketSide.DISPATCH_SCALE,
                TestTickets.weights(),
                retake.fields(),
                List.of(),
                retake.readAt().plusSeconds(MINUTE));

        readings.saveReading(first, ACTOR);
        readings.saveReading(retake, ACTOR);
        readings.saveReading(unreadable, ACTOR);
        readings.saveReading(repeated, ACTOR);

        ShipmentTickets tickets = readings.findTickets(shipment);
        TicketReading dispatch = Objects.requireNonNull(tickets.dispatch());
        assertThat(dispatch.evidenceId()).isEqualTo(retake.evidenceId());
        assertThat(Objects.requireNonNull(dispatch.weights()).netKg()).isEqualByComparingTo("2001");
        assertThat(dispatch.fields()).isEqualTo(retake.fields());
        assertThat(dispatch.problems()).isEmpty();
        assertThat(dispatch.readAt()).isEqualTo(retake.readAt());
        assertThat(tickets.delivery()).isNull();
        assertThat(tickets.reuses()).isEmpty();
        assertThat(readings.findTickets(UUID.randomUUID())).isEqualTo(ShipmentTickets.NONE);
    }

    @Test
    @DisplayName("Aynı fiş no ve kantar başka sevkiyatta okunmuşsa boşluk ve harf büyüklüğü yok sayılarak bulunur")
    void shouldFindTicketReusedAcrossShipments() {
        String ticketNo = uniqueTicket();
        UUID original = UUID.randomUUID();
        UUID copy = UUID.randomUUID();
        TicketFields sameTicketOtherScale = new TicketFields(null, null, ticketNo, "Başka Kantar");
        readings.saveReading(
                reading(original, TestTickets.weights(), fields(" " + ticketNo.toLowerCase(Locale.ROOT)), 0), ACTOR);
        readings.saveReading(reading(UUID.randomUUID(), TestTickets.weights(), sameTicketOtherScale, 0), ACTOR);
        readings.saveReading(reading(copy, TestTickets.weights(), fields(ticketNo), 1), ACTOR);

        ShipmentTickets tickets = readings.findTickets(copy);

        assertThat(tickets.reuses()).containsExactly(new TicketReuse(TicketSide.DISPATCH_SCALE, ticketNo, original));
        assertThat(readings.findTickets(original).reuses()).hasSize(1);
    }

    private static TicketFields fields(String ticketNo) {
        return new TicketFields(TestTickets.PLATE, TestTickets.READ_AT, ticketNo, TestTickets.SCALE);
    }

    private static TicketReading reading(
            UUID shipment, @Nullable ScaleWeights weights, TicketFields fields, long minutesLater) {
        return new TicketReading(
                UUID.randomUUID(),
                shipment,
                TicketSide.DISPATCH_SCALE,
                weights,
                fields,
                weights == null ? List.of("WEIGHT_NOT_READ") : List.of(),
                TestTickets.READ_AT.plusSeconds(minutesLater * MINUTE));
    }

    /** Testler aynı veritabanını paylaştığı için fiş numarası her okumada farklıdır. */
    private static String uniqueTicket() {
        return "T-" + UUID.randomUUID().toString().substring(0, TICKET_LENGTH).toUpperCase(Locale.ROOT);
    }
}
