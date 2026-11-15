package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.UpdateShipmentDateCommand;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateShipmentDateServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-22");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<LocalDate> written = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UpdateShipmentDateService service = new UpdateShipmentDateService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            (id, day, actor, at) -> written.add(day),
            clock);

    private UpdateShipmentDateCommand command(LocalDate day) {
        return new UpdateShipmentDateCommand(TestTokens.actor("manager"), TestShipments.SHIPMENT_ID, day);
    }

    @Test
    @DisplayName("Müdür taslak sevkiyatın gününü bugüne ya da ileri bir güne alır")
    void shouldUpdateDraftDate() {
        queryPort.add(TestShipments.scheduledFor(TODAY.plusDays(1)));

        service.updateShipmentDate(command(TODAY.plusDays(2)));
        service.updateShipmentDate(command(TODAY));

        assertThat(written).containsExactly(TODAY.plusDays(2), TODAY);
    }

    @Test
    @DisplayName("Aynı gün yeniden gönderilirse hiçbir şey yazılmaz")
    void shouldIgnoreSameDate() {
        queryPort.add(TestShipments.scheduledFor(TODAY.plusDays(1)));

        service.updateShipmentDate(command(TODAY.plusDays(1)));

        assertThat(written).isEmpty();
    }

    @Test
    @DisplayName("Geçmiş gün reddedilir")
    void shouldRejectPastDate() {
        queryPort.add(TestShipments.scheduledFor(TODAY.plusDays(1)));

        assertThatThrownBy(() -> service.updateShipmentDate(command(TODAY.minusDays(1))))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThat(written).isEmpty();
    }

    @Test
    @DisplayName("Taslak dışı sevkiyatın günü değişmez")
    void shouldRejectNonDraft() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));

        assertThatThrownBy(() -> service.updateShipmentDate(command(TODAY.plusDays(1))))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(written).isEmpty();
    }
}
