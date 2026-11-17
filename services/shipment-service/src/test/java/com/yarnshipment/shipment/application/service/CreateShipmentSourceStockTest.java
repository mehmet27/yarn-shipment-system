package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import com.yarnshipment.shipment.domain.stock.SourceYarnShortageException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Fasoncudan fasoncuya sevkiyat: gönderen fasoncuda iplik yoksa taslak açılmaz. */
class CreateShipmentSourceStockTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");

    private final List<NewShipment> created = new ArrayList<>();
    private final StubSourceStockPort stock = new StubSourceStockPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final CreateShipmentService service = new CreateShipmentService(
            (shipment, actorUserId) -> {
                created.add(shipment);
                return new CreatedShipment(TestShipments.SHIPMENT_ID, "SV-1");
            },
            new StubSettingsPort(),
            stock,
            new ScopeGuard(event -> {}, clock),
            clock);

    private CreateShipmentCommand command(@Nullable UUID sourceId, List<RecipeLine> lines) {
        ShipmentParties parties = new ShipmentParties(
                UUID.randomUUID(), "Alıcı Tekstil", null, null, null, sourceId, sourceId == null ? null : "Gönderen");
        return new CreateShipmentCommand(
                TestTokens.actor("warehouse"), KEY, TestTokens.SITE_ID, parties, null, lines, null, DAY);
    }

    private static RecipeLine line(int no, String yarn, String kg) {
        return RecipeFixtures.recipeLine(no, null, yarn, ServiceTestSupport.decimal(kg));
    }

    @Test
    @DisplayName("Gönderen fasoncuda iplik yoksa ya da yetmiyorsa taslak açılmaz, eksik kilo bildirilir")
    void shouldRejectWhenSourceLacksYarn() {
        stock.set("36/1 VİSKON", "1000.000");
        List<RecipeLine> lines = List.of(line(1, "36/1 VİSKON", "1500.000"), line(2, "20 denye likra", "100.000"));

        assertThatThrownBy(() -> service.createShipment(command(UUID.randomUUID(), lines)))
                .isInstanceOfSatisfying(SourceYarnShortageException.class, error -> assertThat(error.shortages())
                        .extracting(
                                SourceYarnShortageException.Shortage::yarnType,
                                shortage -> shortage.missingKg().toPlainString())
                        .containsExactly(tuple("36/1 VİSKON", "500.000"), tuple("20 denye likra", "100.000")));
        assertThat(created).isEmpty();
    }

    @Test
    @DisplayName("Aynı iplik iki satırda geçerse kiloları toplanır")
    void shouldSumLinesOfSameYarn() {
        stock.set("36/1 VİSKON", "1000.000");
        List<RecipeLine> lines = List.of(line(1, "36/1 VİSKON", "600.000"), line(2, "36/1 viskon", "600.000"));

        assertThatThrownBy(() -> service.createShipment(command(UUID.randomUUID(), lines)))
                .isInstanceOf(SourceYarnShortageException.class);
    }

    @Test
    @DisplayName("Bakiye yetiyorsa taslak açılır; merkezden çıkan sevkiyatta bakiye aranmaz")
    void shouldCreateWhenSourceCoversOrFromCenter() {
        stock.set("36/1 VİSKON", "2000.000");
        List<RecipeLine> lines = List.of(line(1, "36/1 VİSKON", "1500.000"));

        service.createShipment(command(UUID.randomUUID(), lines));
        service.createShipment(command(null, List.of(line(1, "20 denye likra", "9999.000"))));

        assertThat(created).hasSize(2);
    }
}
