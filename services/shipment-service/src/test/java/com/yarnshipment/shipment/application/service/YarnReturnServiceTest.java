package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fabric.RecordYarnReturnCommand;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.ReturnDirection;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class YarnReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingYarnReturnPort yarnReturns = new RecordingYarnReturnPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");
    private final YarnReturnService service = new YarnReturnService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), yarnReturns, settlements, clock);

    @BeforeEach
    void givenCompletedShipmentWithOpenFile() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        settlements.put(FireSettlement.open(
                TestShipments.SHIPMENT_ID, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("1000.000")));
    }

    private RecordYarnReturnCommand command(ReturnDirection direction, String netKg) {
        return new RecordYarnReturnCommand(
                warehouse, TestShipments.SHIPMENT_ID, direction, "30/1 penye", null, new BigDecimal(netKg), "IRS-9");
    }

    @Test
    @DisplayName("Dönen iplik kaydedilir")
    void shouldRecordReturnedYarn() {
        // when
        YarnReturn record = service.recordYarnReturn(command(ReturnDirection.RETURN, "100"));

        // then
        assertThat(yarnReturns.saved()).containsExactly(record);
        assertThat(record.createdAt()).isEqualTo(NOW);
        assertThat(record.waybillNo()).isEqualTo("IRS-9");
    }

    @Test
    @DisplayName("Dönen iplik toplamı gönderilen kiloyu aşamaz; haklı itiraz kilosu düşülür")
    void shouldRejectReturnAboveSentKg() {
        // given
        settlements.givenTotals("900", "0", "0");

        // when / then
        assertThatThrownBy(() -> service.recordYarnReturn(command(ReturnDirection.RETURN, "100.001")))
                .isInstanceOf(InvalidLedgerInputException.class);
        service.recordYarnReturn(command(ReturnDirection.RETURN, "100"));
        assertThat(yarnReturns.saved()).hasSize(1);
    }

    @Test
    @DisplayName("Fasoncudan gelen iplik fire tabanı sınırına tabi değildir")
    void shouldNotLimitYarnFromSubcontractor() {
        // given
        settlements.givenTotals("1000", "0", "0");

        // when
        service.recordYarnReturn(command(ReturnDirection.FROM_SUBCONTRACTOR, "50"));

        // then
        assertThat(yarnReturns.saved())
                .singleElement()
                .satisfies(record -> assertThat(record.direction()).isEqualTo(ReturnDirection.FROM_SUBCONTRACTOR));
    }

    @Test
    @DisplayName("Tamamlanmamış sevkiyata iplik dönüşü yazılamaz")
    void shouldRejectUncompletedShipment() {
        // given
        InMemoryShipmentQueryPort other = new InMemoryShipmentQueryPort();
        other.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        YarnReturnService awaiting = new YarnReturnService(
                new ScopedShipmentLoader(other, new ScopeGuard(event -> {}, clock)), yarnReturns, settlements, clock);

        // when / then
        assertThatThrownBy(() -> awaiting.recordYarnReturn(command(ReturnDirection.RETURN, "10")))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
