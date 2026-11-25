package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.fabric.RecordFabricReceiptCommand;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.fabric.FabricReceipt;
import com.yarnshipment.shipment.domain.fabric.FabricYarnShortageException;
import com.yarnshipment.shipment.domain.fabric.YarnDraw;
import com.yarnshipment.shipment.domain.fabric.YarnRatio;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// İstisna: CPLX-09/CPLX-10, senaryo testi alanın tüm port sahtelerini ve domain tiplerini birlikte kurar (GIT-10)
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
class FabricReceiptServiceTest {

    private static final int ROLLS = 5;
    private static final Duration TOLERANCE = Duration.ofMinutes(5);
    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");
    private static final UUID OLDER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");
    private static final UUID NEWER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a2");
    private static final String PENYE = "30/1 penye";
    private static final String OPEN_END = "20/1 open end";

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingLedgerPorts ledger = new RecordingLedgerPorts();
    private final InMemoryYarnStock stock = new InMemoryYarnStock();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor quality = TestTokens.actor("quality");
    private final FabricReceiptService service = new FabricReceiptService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), ledger, settlements, stock, clock);

    @BeforeEach
    void givenCompletedShipmentWithOpenFile() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        open(TestShipments.SHIPMENT_ID);
        open(OLDER);
        open(NEWER);
        stock.givenYarn(TestShipments.SHIPMENT_ID, "SV-3", "2026-11-03T08:00:00Z", PENYE, "600");
        stock.givenYarn(TestShipments.SHIPMENT_ID, "SV-3", "2026-11-03T08:00:00Z", OPEN_END, "400");
        stock.givenYarn(OLDER, "SV-1", "2026-11-01T08:00:00Z", PENYE, "300");
        stock.givenYarn(NEWER, "SV-2", "2026-11-02T08:00:00Z", PENYE, "200");
    }

    private void open(UUID shipmentId) {
        settlements.put(FireSettlement.open(shipmentId, TestShipments.SUBCONTRACTOR_ID, new BigDecimal("1000.000")));
    }

    private static YarnRatio ratio(String yarn, String pct) {
        return new YarnRatio(yarn, null, new BigDecimal(pct));
    }

    private RecordFabricReceiptCommand command(String netKg, YarnRatio... yarns) {
        return new RecordFabricReceiptCommand(
                quality,
                TestShipments.SHIPMENT_ID,
                "Süprem",
                new BigDecimal(netKg),
                ROLLS,
                "KIR-1",
                null,
                List.of(yarns));
    }

    private FireSettlementStatus statusOf(UUID shipmentId) {
        return settlements.find(shipmentId).orElseThrow().status();
    }

    @Test
    @DisplayName("Oranlara göre kilo bölünür; iplik önce kumaşın girildiği sevkiyattan düşer ve yalnız o dosya ilerler")
    void shouldDrawFromEntryShipmentFirst() {
        // when
        FabricReceipt receipt = service.recordFabricReceipt(command("960", ratio(PENYE, "60"), ratio(OPEN_END, "40")));

        // then
        assertThat(receipt.yarns()).hasSize(2);
        assertThat(receipt.yarns().getFirst().netKg()).isEqualByComparingTo("576");
        assertThat(receipt.yarns().get(1).netKg()).isEqualByComparingTo("384");
        assertThat(receipt.yarns().getFirst().draws())
                .singleElement()
                .satisfies(draw -> assertThat(draw.shipmentId()).isEqualTo(TestShipments.SHIPMENT_ID));
        assertThat(receipt.receivedAt()).isEqualTo(NOW);
        assertThat(ledger.receipts()).containsExactly(receipt);
        assertThat(statusOf(TestShipments.SHIPMENT_ID)).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(statusOf(OLDER)).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
        assertThat(statusOf(NEWER)).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
    }

    @Test
    @DisplayName("Girildiği sevkiyatta iplik yetmezse kalan kısım en eski sevkiyattan başlayarak düşer; hepsi ilerler")
    void shouldSpillOverToOldestShipments() {
        // when: 800 kg penye gerekir; girildiği sevkiyatta 600, eski sevkiyatta 300, yeni sevkiyatta 200 var
        FabricReceipt receipt = service.recordFabricReceipt(command("800", ratio(PENYE, "100")));

        // then
        List<YarnDraw> draws = receipt.yarns().getFirst().draws();
        assertThat(draws).extracting(YarnDraw::shipmentId).containsExactly(TestShipments.SHIPMENT_ID, OLDER);
        assertThat(draws.get(0).netKg()).isEqualByComparingTo("600");
        assertThat(draws.get(1).netKg()).isEqualByComparingTo("200");
        assertThat(statusOf(TestShipments.SHIPMENT_ID)).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(statusOf(OLDER)).isEqualTo(FireSettlementStatus.TO_INVOICE);
        assertThat(statusOf(NEWER)).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
    }

    @Test
    @DisplayName("Fasoncunun toplamında iplik yetmezse kayıt engellenir ve hangi iplikten kaç kg eksik olduğu söylenir")
    void shouldBlockWhenYarnIsShort() {
        // when / then: penye toplamı 1100; 1200 kg kumaşın %100'ü penye
        assertThatThrownBy(() -> service.recordFabricReceipt(command("1200", ratio(PENYE, "100"))))
                .isInstanceOfSatisfying(
                        FabricYarnShortageException.class,
                        e -> assertThat(e.shortages()).singleElement().satisfies(shortage -> {
                            assertThat(shortage.yarnType()).isEqualTo(PENYE);
                            assertThat(shortage.missingKg()).isEqualByComparingTo("100");
                        }));
        assertThat(ledger.receipts()).isEmpty();
        assertThat(statusOf(OLDER)).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
    }

    @Test
    @DisplayName("Fasoncuda hiç olmayan iplik de eksik sayılır")
    void shouldBlockUnknownYarn() {
        assertThatThrownBy(() -> service.recordFabricReceipt(command("10", ratio("yok", "100"))))
                .isInstanceOfSatisfying(
                        FabricYarnShortageException.class,
                        e -> assertThat(e.shortages())
                                .singleElement()
                                .satisfies(shortage ->
                                        assertThat(shortage.missingKg()).isEqualByComparingTo("10")));
    }

    @Test
    @DisplayName("Oranların toplamı tam 100 olmalı; aynı iplik iki kez yazılamaz; oran yazılmadan kayıt olmaz")
    void shouldRequireRatiosToSumToHundred() {
        assertThatThrownBy(() -> service.recordFabricReceipt(command("100", ratio(PENYE, "99.99"))))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() ->
                        service.recordFabricReceipt(command("100", ratio(PENYE, "60"), ratio(" 30/1 PENYE", "40"))))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> service.recordFabricReceipt(command("100")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(ledger.receipts()).isEmpty();
    }

    @Test
    @DisplayName("Büyük harfle ya da boşluklu yazılan Türkçe iplik adı eşleşir")
    void shouldMatchTurkishYarnName() {
        stock.givenYarn(TestShipments.SHIPMENT_ID, "SV-3", "2026-11-03T08:00:00Z", "İpek ıslak", "100");

        FabricReceipt receipt = service.recordFabricReceipt(command("10", ratio(" İPEK ISLAK ", "100")));

        assertThat(receipt.yarns())
                .singleElement()
                .satisfies(yarn -> assertThat(yarn.yarnType()).isEqualTo("İpek ıslak"));
    }

    @Test
    @DisplayName("Geliş zamanı şimdiden 5 dakikadan fazla ileride olamaz; sınırdaki ve geçmiş zaman kabul edilir")
    void shouldRejectFutureReceivedAt() {
        assertThatThrownBy(() -> service.recordFabricReceipt(
                        commandAt(NOW.plus(TOLERANCE).plusSeconds(1))))
                .isInstanceOfSatisfying(
                        InvalidLedgerInputException.class,
                        e -> assertThat(e.field()).isEqualTo("receivedAt"));
        assertThat(ledger.receipts()).isEmpty();

        FabricReceipt edge = service.recordFabricReceipt(commandAt(NOW.plus(TOLERANCE)));
        FabricReceipt past = service.recordFabricReceipt(commandAt(NOW.minus(Duration.ofDays(1))));

        assertThat(edge.receivedAt()).isEqualTo(NOW.plus(TOLERANCE));
        assertThat(past.receivedAt()).isEqualTo(NOW.minus(Duration.ofDays(1)));
    }

    private RecordFabricReceiptCommand commandAt(Instant receivedAt) {
        return new RecordFabricReceiptCommand(
                quality,
                TestShipments.SHIPMENT_ID,
                "Süprem",
                BigDecimal.TEN,
                ROLLS,
                "KIR-1",
                receivedAt,
                List.of(ratio(PENYE, "100")));
    }

    @Test
    @DisplayName("Tamamlanmamış sevkiyata kumaş girişi yapılamaz")
    void shouldRejectUncompletedShipment() {
        // given
        InMemoryShipmentQueryPort other = new InMemoryShipmentQueryPort();
        other.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        FabricReceiptService awaiting = new FabricReceiptService(
                new ScopedShipmentLoader(other, new ScopeGuard(event -> {}, clock)), ledger, settlements, stock, clock);

        // when / then
        assertThatThrownBy(() -> awaiting.recordFabricReceipt(command("10", ratio(PENYE, "100"))))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(ledger.receipts()).isEmpty();
    }

    @Test
    @DisplayName("Kapanmış fire dosyası olan sevkiyat üzerinden kumaş girilemez")
    void shouldRejectReceiptOnClosedFile() {
        // given
        FireSettlement open = settlements.find(TestShipments.SHIPMENT_ID).orElseThrow();
        FireCalculation withinFire = FireCalculation.of(
                new Inputs(open.sentKg(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("960"), open.fireRate()));
        settlements.put(open.afterFabricReceipt().close(withinFire));

        // when / then
        assertThatThrownBy(() -> service.recordFabricReceipt(command("10", ratio(PENYE, "100"))))
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
                stock,
                clock);

        // when / then
        assertThatThrownBy(() -> withoutFile.recordFabricReceipt(command("10", ratio(PENYE, "100"))))
                .isInstanceOf(FireSettlementNotFoundException.class);
    }
}
