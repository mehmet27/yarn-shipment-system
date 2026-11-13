package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.FabricExceedsSentYarnException;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.fire.FireCalculation;
import com.yarnshipment.shipment.domain.fire.FireCalculation.Inputs;
import com.yarnshipment.shipment.domain.fire.FireSettlement;
import com.yarnshipment.shipment.domain.fire.FireSettlementClosedException;
import com.yarnshipment.shipment.domain.fire.FireSettlementNotFoundException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class FabricReceiptServiceTest {

    private static final int ROLLS = 5;
    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingLedgerPorts ledger = new RecordingLedgerPorts();
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor quality = TestTokens.actor("quality");
    private final FabricReceiptService service = new FabricReceiptService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            ledger,
            settlements,
            ledger,
            events,
            clock);

    @BeforeEach
    void givenCompletedShipmentWithOpenFile() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        settlements.put(FireSettlement.open(
                TestShipments.SHIPMENT_ID, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("1000.000")));
        ledger.givenRecipe("30/1 penye", "600");
        ledger.givenRecipe("20/1 open end", "400");
    }

    private RecordFabricReceiptCommand command(String netKg, @Nullable List<YarnShare> yarns) {
        return new RecordFabricReceiptCommand(
                quality, TestShipments.SHIPMENT_ID, "Süprem", new BigDecimal(netKg), ROLLS, "KIR-1", null, yarns);
    }

    @Test
    @DisplayName("İplik verilmezse kumaş reçete oranıyla dağıtılır, kayıt yazılır ve dosya faturalanmaya hazır olur")
    void shouldDistributeByRecipeAndAdvanceFile() {
        // when
        FabricReceipt receipt = service.recordFabricReceipt(command("960", null));

        // then
        assertThat(receipt.yarns()).extracting(YarnShare::yarnType).containsExactly("30/1 penye", "20/1 open end");
        assertThat(receipt.yarns().get(0).netKg()).isEqualByComparingTo("576");
        assertThat(receipt.yarns().get(1).netKg()).isEqualByComparingTo("384");
        assertThat(receipt.receivedAt()).isEqualTo(NOW);
        assertThat(ledger.receipts()).containsExactly(receipt);
        assertThat(events.names).containsExactly("fabricReceived");
        assertThat(settlements.find(TestShipments.SHIPMENT_ID))
                .get()
                .extracting(FireSettlement::status)
                .isEqualTo(FireSettlementStatus.TO_INVOICE);
    }

    @Test
    @DisplayName("Açıkça verilen iplikler kumaş kilosuna eşit olmalıdır; tekrarlanan cins birleştirilir")
    void shouldRequireExplicitYarnsToSumToNetKg() {
        // when
        FabricReceipt receipt = service.recordFabricReceipt(command(
                "100", List.of(new YarnShare("A", new BigDecimal("70")), new YarnShare("A", new BigDecimal("30")))));

        // then
        assertThat(receipt.yarns())
                .singleElement()
                .satisfies(share -> assertThat(share.netKg()).isEqualByComparingTo("100"));
        assertThatThrownBy(() ->
                        service.recordFabricReceipt(command("100", List.of(new YarnShare("A", new BigDecimal("90"))))))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(ledger.receipts()).hasSize(1);
    }

    @Test
    @DisplayName("Gelen kumaşın toplamı gönderilen iplikten fazla olamaz")
    void shouldRejectFabricExceedingSentYarn() {
        assertThatThrownBy(() -> service.recordFabricReceipt(command("1000.001", null)))
                .isInstanceOf(FabricExceedsSentYarnException.class);
        service.recordFabricReceipt(command("600", null));
        assertThatThrownBy(() -> service.recordFabricReceipt(command("500", null)))
                .isInstanceOf(FabricExceedsSentYarnException.class);
        assertThat(ledger.receipts()).hasSize(1);
    }

    @Test
    @DisplayName("Tamamlanmamış sevkiyata kumaş girişi yapılamaz")
    void shouldRejectUncompletedShipment() {
        // given
        InMemoryShipmentQueryPort other = new InMemoryShipmentQueryPort();
        other.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        FabricReceiptService awaiting = new FabricReceiptService(
                new ScopedShipmentLoader(other, new ScopeGuard(event -> {}, clock)),
                ledger,
                settlements,
                ledger,
                events,
                clock);

        // when / then
        assertThatThrownBy(() -> awaiting.recordFabricReceipt(command("10", null)))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(ledger.receipts()).isEmpty();
    }

    @Test
    @DisplayName("Kapanmış fire dosyasına kumaş girişi yapılamaz")
    void shouldRejectReceiptOnClosedFile() {
        // given
        FireSettlement open = settlements.find(TestShipments.SHIPMENT_ID).orElseThrow();
        FireCalculation withinFire = FireCalculation.of(
                new Inputs(open.sentKg(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("960"), open.fireRate()));
        settlements.put(open.afterFabricReceipt().close(withinFire));

        // when / then
        assertThatThrownBy(() -> service.recordFabricReceipt(command("10", null)))
                .isInstanceOf(FireSettlementClosedException.class);
        assertThat(ledger.receipts()).isEmpty();
    }

    @Test
    @DisplayName("Fire dosyası olmayan sevkiyata kumaş girişi 404 sınıfı hata verir")
    void shouldRejectWhenNoFireFile() {
        // given
        FabricReceiptService withoutFile = new FabricReceiptService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
                ledger,
                new InMemoryFireSettlementPort(),
                ledger,
                events,
                clock);

        // when / then
        assertThatThrownBy(() -> withoutFile.recordFabricReceipt(command("10", null)))
                .isInstanceOf(FireSettlementNotFoundException.class);
    }
}
