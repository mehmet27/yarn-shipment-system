package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Sevkiyat günü yalnızca taslak sevkiyatta, gerçek veritabanında güncellenir. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    RecordingDomainEventPublisher.class,
    ShipmentCreationAdapter.class,
    ShipmentScheduleAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
class ShipmentScheduleAdapterTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000c001");
    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final LocalDate DAY = LocalDate.parse("2026-10-22");

    @Autowired
    private ShipmentCreationAdapter creation;

    @Autowired
    private ShipmentScheduleAdapter schedule;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    private CreatedShipment draft() {
        ShipmentParties parties = new ShipmentParties(SUBCONTRACTOR, "Örnek Tekstil", null, null, null);
        List<RecipeLine> lines = List.of(new RecipeLine(1, null, "36/1 VİSKON", null, new BigDecimal("100.000")));
        return creation.create(new NewShipment(SITE, parties, null, lines, new BigDecimal("0.0400"), DAY, NOW), USER);
    }

    private LocalDate dateOf(UUID id) {
        return Objects.requireNonNull(
                jdbc.queryForObject("SELECT shipment_date FROM shipment WHERE id = ?", LocalDate.class, id));
    }

    @Test
    @DisplayName("Taslak sevkiyatın günü değişir")
    void shouldUpdateDraftDate() {
        CreatedShipment created = draft();

        schedule.updateShipmentDate(created.id(), DAY.plusDays(2), USER, NOW);

        assertThat(dateOf(created.id())).isEqualTo(DAY.plusDays(2));
    }

    @Test
    @DisplayName("Taslak olmayan ya da olmayan sevkiyatın günü değişmez")
    void shouldRejectNonDraft() {
        CreatedShipment created = draft();
        jdbc.update("UPDATE shipment SET status = 'CANCELLED', cancelled_reason = 'x' WHERE id = ?", created.id());

        assertThatThrownBy(() -> schedule.updateShipmentDate(created.id(), DAY.plusDays(1), USER, NOW))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(dateOf(created.id())).isEqualTo(DAY);
    }
}
