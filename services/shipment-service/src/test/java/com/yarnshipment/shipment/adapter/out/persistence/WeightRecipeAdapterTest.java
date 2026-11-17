package com.yarnshipment.shipment.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.events.testing.RecordingDomainEventPublisher;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort.Audit;
import com.yarnshipment.shipment.application.port.out.WeightRecipePort.RecipeReplacement;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

/** Düzeltilen reçete gerçek veritabanında yazılır: satır yerinde düzelir, fazlası eklenir, sevkiyat yola çıkar. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    RecordingDomainEventPublisher.class,
    ShipmentCreationAdapter.class,
    WeightRecipeAdapter.class,
    ShipmentTransitionWriter.class,
    UuidV7Generator.class
})
class WeightRecipeAdapterTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000c001");
    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");

    @Autowired
    private ShipmentCreationAdapter creation;

    @Autowired
    private WeightRecipeAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.register(registry);
    }

    @Test
    @DisplayName("Mevcut satır düzelir, yeni satır eklenir, beklenen net toplama eşitlenir ve sevkiyat yola çıkar")
    void shouldReplaceLinesAndDispatch() {
        ShipmentParties parties = new ShipmentParties(SUBCONTRACTOR, "Örnek Tekstil", null, null, null);
        List<RecipeLine> original = List.of(new RecipeLine(1, null, "36/1 VİSKON", null, new BigDecimal("100.000")));
        UUID shipmentId = creation.create(
                        new NewShipment(
                                SITE,
                                parties,
                                null,
                                original,
                                new BigDecimal("0.0400"),
                                LocalDate.parse("2026-11-11"),
                                NOW),
                        USER)
                .id();
        jdbc.update("UPDATE shipment SET status = 'WEIGHT_MISMATCH' WHERE id = ?", shipmentId);
        List<RecipeLine> edited = List.of(
                new RecipeLine(1, null, "30/1 POLY", null, new BigDecimal("60.000")),
                new RecipeLine(2, null, "36/1 VİSKON", null, new BigDecimal("30.500")));
        ShipmentDispatched event =
                new ShipmentDispatched(shipmentId, SITE, SUBCONTRACTOR, 2, new BigDecimal("90.500"), NOW);

        adapter.replaceAndDispatch(
                new RecipeReplacement(shipmentId, edited, new BigDecimal("90.500"), event),
                "Reçete düzeltildi",
                new Audit(USER, NOW));

        assertThat(jdbc.queryForList(
                        "SELECT yarn_type_text FROM shipment_line WHERE shipment_id = ? ORDER BY line_no",
                        String.class,
                        shipmentId))
                .containsExactly("30/1 POLY", "36/1 VİSKON");
        assertThat(jdbc.queryForObject("SELECT status FROM shipment WHERE id = ?", String.class, shipmentId))
                .isEqualTo("AWAITING_DELIVERY");
        assertThat(jdbc.queryForObject(
                        "SELECT planned_net_kg FROM shipment WHERE id = ?", BigDecimal.class, shipmentId))
                .isEqualByComparingTo("90.500");
    }
}
