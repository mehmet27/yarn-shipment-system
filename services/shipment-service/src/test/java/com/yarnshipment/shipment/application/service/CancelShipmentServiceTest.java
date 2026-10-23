package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.out.ShipmentCancellationPort;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.cancellation.Cancellation;
import com.yarnshipment.shipment.domain.cancellation.InvalidCancellationException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelShipmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final int TOO_LONG = 501;

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<Cancellation> cancellations = new ArrayList<>();
    private final RecordingBagReservationPort bags = new RecordingBagReservationPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor admin = TestTokens.actor("admin");

    private CancelShipmentService service() {
        ShipmentCancellationPort port = (cancellation, actorUserId) -> cancellations.add(cancellation);
        return new CancelShipmentService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), port, bags, clock);
    }

    private CancelShipmentCommand command(String reason) {
        return new CancelShipmentCommand(admin, TestShipments.SHIPMENT_ID, reason, UUID.randomUUID());
    }

    @Test
    @DisplayName("Taslak ya da çıkış yapılmış sevkiyat iptal edilir ve çuvalları serbest bırakılır")
    void shouldCancelAndReleaseBags() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));

        service().cancelShipment(command("  Yanlış sevkiyat  "));

        assertThat(cancellations).singleElement().satisfies(cancellation -> {
            assertThat(cancellation.reason()).isEqualTo("Yanlış sevkiyat");
            assertThat(cancellation.fromStatus()).isEqualTo(ShipmentStatus.LOADED);
            assertThat(cancellation.cancelledAt()).isEqualTo(NOW);
        });
        assertThat(bags.released()).containsExactly(TestShipments.SHIPMENT_ID);
    }

    @Test
    @DisplayName("Yola çıkmış sevkiyat iptal edilemez; çuval servisine gidilmez")
    void shouldRejectShipmentAlreadyOnTheRoad() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));
        CancelShipmentService service = service();
        CancelShipmentCommand command = command("Gerekçe");

        assertThatThrownBy(() -> service.cancelShipment(command)).isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(cancellations).isEmpty();
        assertThat(bags.released()).isEmpty();
    }

    @Test
    @DisplayName("Boş ya da çok uzun gerekçe reddedilir")
    void shouldRejectBadReason() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        CancelShipmentService service = service();
        CancelShipmentCommand blank = command("   ");
        CancelShipmentCommand tooLong = command("x".repeat(TOO_LONG));

        assertThatThrownBy(() -> service.cancelShipment(blank)).isInstanceOf(InvalidCancellationException.class);
        assertThatThrownBy(() -> service.cancelShipment(tooLong)).isInstanceOf(InvalidCancellationException.class);
        assertThat(cancellations).isEmpty();
    }

    @Test
    @DisplayName("Zaten iptal edilmiş sevkiyata gelen istek başarılı döner ve tekrar yazmaz")
    void shouldTreatRepeatedCancellationAsSuccess() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.CANCELLED));

        service().cancelShipment(command("Gerekçe"));

        assertThat(cancellations).isEmpty();
        assertThat(bags.released()).isEmpty();
    }

    @Test
    @DisplayName("Çuval servisi bırakamazsa istisna yayılır (transaction iptali geri alır)")
    void shouldPropagateReleaseFailure() throws Exception {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        bags.failOnRelease();
        CancelShipmentService service = service();
        CancelShipmentCommand command = command("Gerekçe");

        assertThatThrownBy(() -> service.cancelShipment(command)).isInstanceOf(BagServiceUnavailableException.class);
    }

    @Test
    @DisplayName("Olmayan sevkiyat 404 verir")
    void shouldFailForMissingShipment() throws Exception {
        CancelShipmentService service = service();
        CancelShipmentCommand command = command("Gerekçe");

        assertThatThrownBy(() -> service.cancelShipment(command)).isInstanceOf(ShipmentNotFoundException.class);
    }
}
