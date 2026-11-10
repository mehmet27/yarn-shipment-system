package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.application.port.out.ShipmentCreationPort;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.NewShipment;
import com.yarnshipment.shipment.domain.model.ShipmentParties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateShipmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000ff");

    private static final BigDecimal FIRE = new BigDecimal("0.0400");
    private static final List<RecipeLine> LINES = List.of(
            new RecipeLine(1, null, "36/1 VİSKON", null, new BigDecimal("19488.000")),
            new RecipeLine(2, UUID.randomUUID(), null, null, new BigDecimal("13492.000")));

    private final List<NewShipment> created = new ArrayList<>();
    private final ShipmentCreationPort port = (shipment, actorUserId) -> {
        created.add(shipment);
        return new CreatedShipment(TestShipments.SHIPMENT_ID, "SV-1");
    };
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final CreateShipmentService service =
            new CreateShipmentService(port, new StubSettingsPort(), new ScopeGuard(event -> {}, clock), clock);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private CreateShipmentCommand command(UUID siteId) {
        return command(siteId, null);
    }

    private CreateShipmentCommand command(UUID siteId, @Nullable BigDecimal fireRate) {
        ShipmentParties parties =
                new ShipmentParties(TestShipments.SUBCONTRACTOR_ID, "Örnek Tekstil", null, null, null);
        return new CreateShipmentCommand(warehouse, KEY, siteId, parties, null, LINES, fireRate);
    }

    @Test
    @DisplayName("Yetkili lokasyonda taslak sunucu saatiyle oluşturulur")
    void shouldCreateDraftInAuthorizedSite() {
        CreatedShipment result = service.createShipment(command(TestTokens.SITE_ID));

        assertThat(result.shipmentNo()).isEqualTo("SV-1");
        assertThat(created).singleElement().satisfies(shipment -> {
            assertThat(shipment.createdAt()).isEqualTo(NOW);
            assertThat(shipment.siteId()).isEqualTo(TestTokens.SITE_ID);
        });
    }

    @Test
    @DisplayName("Beklenen net reçete satırlarının toplamıdır; fire oranı verilmezse ayardaki varsayılan kullanılır")
    void shouldSumRecipeAndDefaultFireRate() {
        service.createShipment(command(TestTokens.SITE_ID));

        assertThat(created).singleElement().satisfies(shipment -> {
            assertThat(shipment.plannedNetKg()).isEqualByComparingTo("32980.000");
            assertThat(shipment.fireRate()).isEqualByComparingTo("0.0400");
        });
    }

    @Test
    @DisplayName("Verilen fire oranı varsayılanın yerine geçer; 0.20 üstü, reçetesiz ve ölçüsüz değerler reddedilir")
    void shouldUseGivenFireRateAndRejectInvalidInput() {
        service.createShipment(command(TestTokens.SITE_ID, new BigDecimal("0.0750")));

        assertThat(created).singleElement().satisfies(shipment -> assertThat(shipment.fireRate())
                .isEqualByComparingTo("0.0750"));
        assertThatThrownBy(() -> service.createShipment(command(TestTokens.SITE_ID, new BigDecimal("0.2100"))))
                .isInstanceOf(InvalidNewShipmentException.class);
        ShipmentParties parties = new ShipmentParties(TestShipments.SUBCONTRACTOR_ID, "Örnek", null, null, null);
        assertThatThrownBy(() -> new NewShipment(TestTokens.SITE_ID, parties, null, List.of(), FIRE, NOW))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new RecipeLine(1, null, "x", null, BigDecimal.ZERO))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new RecipeLine(1, null, null, null, BigDecimal.ONE))
                .isInstanceOf(InvalidNewShipmentException.class);
    }

    @Test
    @DisplayName("Yetkisiz lokasyonda oluşturma reddedilir ve kayıt yazılmaz")
    void shouldRejectUnauthorizedSite() {
        assertThatThrownBy(() -> service.createShipment(command(OTHER_SITE)))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(created).isEmpty();
    }

    @Test
    @DisplayName("Şoför kimliği adsız ya da araç kimliği plakasız verilirse reddedilir")
    void shouldRejectUnpairedDriverOrVehicle() {
        ShipmentParties noName =
                new ShipmentParties(TestShipments.SUBCONTRACTOR_ID, "Örnek", UUID.randomUUID(), null, null);
        ShipmentParties plateOnly =
                new ShipmentParties(TestShipments.SUBCONTRACTOR_ID, "Örnek", null, null, "34ABC123");

        assertThatThrownBy(() -> new NewShipment(TestTokens.SITE_ID, noName, null, LINES, FIRE, NOW))
                .isInstanceOf(InvalidNewShipmentException.class)
                .extracting(e -> ((InvalidNewShipmentException) e).field())
                .isEqualTo("driverId");
        assertThatThrownBy(() -> new NewShipment(TestTokens.SITE_ID, plateOnly, null, LINES, FIRE, NOW))
                .isInstanceOf(InvalidNewShipmentException.class)
                .extracting(e -> ((InvalidNewShipmentException) e).field())
                .isEqualTo("vehicleId");
    }
}
