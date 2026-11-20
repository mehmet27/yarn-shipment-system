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

// İstisna: CPLX-09/CPLX-10, senaryo testi alanın tüm port sahtelerini ve domain tiplerini birlikte kurar (GIT-10)
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class YarnReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingYarnReturnPort yarnReturns = new RecordingYarnReturnPort();
    private final RecordingLedgerPorts ledger = new RecordingLedgerPorts();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor warehouse = TestTokens.actor("warehouse");
    private final YarnReturnService service = new YarnReturnService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            yarnReturns,
            settlements,
            ledger,
            clock);

    @BeforeEach
    void givenCompletedShipmentWithOpenFile() {
        ledger.givenRecipe("30/1 penye", "1000.000");
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        settlements.put(FireSettlement.open(
                TestShipments.SHIPMENT_ID, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("1000.000")));
    }

    private RecordYarnReturnCommand command(String netKg) {
        return new RecordYarnReturnCommand(
                warehouse, TestShipments.SHIPMENT_ID, "30/1 penye", null, new BigDecimal(netKg), "IRS-9");
    }

    @Test
    @DisplayName("Dönen iplik kaydedilir")
    void shouldRecordReturnedYarn() {
        // when
        YarnReturn record = service.recordYarnReturn(command("100"));

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
        assertThatThrownBy(() -> service.recordYarnReturn(command("100.001")))
                .isInstanceOf(InvalidLedgerInputException.class);
        service.recordYarnReturn(command("100"));
        assertThat(yarnReturns.saved()).hasSize(1);
    }

    @Test
    @DisplayName("Dönen iplik gönderilen iplikler arasından olmalı ve o ipliğin kilosunu (öncekiler düşülerek) aşamaz")
    void shouldLimitReturnPerSentYarn() {
        // given
        ledger.givenRecipe("20/1 open end", "300.000");

        // when / then
        service.recordYarnReturn(command("1000"));
        assertThatThrownBy(() -> service.recordYarnReturn(command("0.001")))
                .isInstanceOf(InvalidLedgerInputException.class);
        RecordYarnReturnCommand unknown = new RecordYarnReturnCommand(
                warehouse, TestShipments.SHIPMENT_ID, "40/1 penye", null, new BigDecimal("1"), null);
        assertThatThrownBy(() -> service.recordYarnReturn(unknown)).isInstanceOf(InvalidLedgerInputException.class);
        RecordYarnReturnCommand other = new RecordYarnReturnCommand(
                warehouse, TestShipments.SHIPMENT_ID, "20/1 open end", null, new BigDecimal("300"), null);
        assertThat(service.recordYarnReturn(other).yarnType()).isEqualTo("20/1 open end");
    }

    @Test
    @DisplayName("Kayıt her zaman fasoncudan depoya dönüş olarak yazılır")
    void shouldAlwaysRecordAsReturn() {
        // when
        service.recordYarnReturn(command("50"));

        // then
        assertThat(yarnReturns.saved())
                .singleElement()
                .satisfies(record -> assertThat(record.direction()).isEqualTo(ReturnDirection.RETURN));
    }

    @Test
    @DisplayName("Tamamlanmamış sevkiyata iplik dönüşü yazılamaz")
    void shouldRejectUncompletedShipment() {
        // given
        InMemoryShipmentQueryPort other = new InMemoryShipmentQueryPort();
        other.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        YarnReturnService awaiting = new YarnReturnService(
                new ScopedShipmentLoader(other, new ScopeGuard(event -> {}, clock)),
                yarnReturns,
                settlements,
                ledger,
                clock);

        // when / then
        assertThatThrownBy(() -> awaiting.recordYarnReturn(command("10")))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }
}
