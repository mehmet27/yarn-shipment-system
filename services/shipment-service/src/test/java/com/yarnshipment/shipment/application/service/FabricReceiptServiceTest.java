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
import com.yarnshipment.shipment.domain.fabric.FabricYarnNotInShipmentException;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// İstisna: CPLX-09/CPLX-10, senaryo testi alanın tüm port sahtelerini ve domain tiplerini birlikte kurar (GIT-10)
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
class FabricReceiptServiceTest {

    private static final int ROLLS = 5;
    private static final Duration TOLERANCE = Duration.ofMinutes(5);
    private static final Instant NOW = Instant.parse("2026-11-12T09:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final InMemoryFireSettlementPort settlements = new InMemoryFireSettlementPort();
    private final RecordingLedgerPorts ledger = new RecordingLedgerPorts();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor quality = TestTokens.actor("quality");
    private final FabricReceiptService service = new FabricReceiptService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            ledger,
            settlements,
            ledger,
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
        assertThat(receipt.yarns().getFirst().netKg()).isEqualByComparingTo("576");
        assertThat(receipt.yarns().get(1).netKg()).isEqualByComparingTo("384");
        assertThat(receipt.receivedAt()).isEqualTo(NOW);
        assertThat(ledger.receipts()).containsExactly(receipt);
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
                "100",
                List.of(
                        new YarnShare("30/1 penye", new BigDecimal("70")),
                        new YarnShare("30/1 PENYE ", new BigDecimal("30")))));

        // then
        assertThat(receipt.yarns()).singleElement().satisfies(share -> {
            assertThat(share.netKg()).isEqualByComparingTo("100");
            assertThat(share.yarnType()).isEqualTo("30/1 penye");
        });
        assertThatThrownBy(() -> service.recordFabricReceipt(
                        command("100", List.of(new YarnShare("30/1 penye", new BigDecimal("90"))))))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(ledger.receipts()).hasSize(1);
    }

    @Test
    @DisplayName("Sevkiyatta gönderilmemiş iplik cinsi reddedilir; kayıt yazılmaz")
    void shouldRejectYarnNotInShipment() {
        assertThatThrownBy(() -> service.recordFabricReceipt(
                        command("100", List.of(new YarnShare("30/1 penyee", new BigDecimal("100"))))))
                .isInstanceOf(FabricYarnNotInShipmentException.class);
        assertThat(ledger.receipts()).isEmpty();
    }

    @Test
    @DisplayName("Büyük harfle ya da boşluklu yazılan Türkçe cins gönderilen cinsin yazılışına çevrilir")
    void shouldCanonicaliseTurkishYarnName() {
        ledger.givenRecipe("İpek ıslak", "100");

        FabricReceipt receipt = service.recordFabricReceipt(
                command("10", List.of(new YarnShare(" İPEK ISLAK ", new BigDecimal("10")))));

        assertThat(receipt.yarns())
                .singleElement()
                .satisfies(share -> assertThat(share.yarnType()).isEqualTo("İpek ıslak"));
    }

    @Test
    @DisplayName("Cins bilgisi olmayan eski sevkiyatta iplik adı denetlenmez")
    void shouldSkipYarnCheckWithoutDispatchData() {
        RecordingLedgerPorts empty = new RecordingLedgerPorts();
        FabricReceiptService legacy = new FabricReceiptService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
                empty,
                settlements,
                empty,
                clock);

        FabricReceipt receipt =
                legacy.recordFabricReceipt(command("10", List.of(new YarnShare("serbest", new BigDecimal("10")))));

        assertThat(receipt.yarns()).extracting(YarnShare::yarnType).containsExactly("serbest");
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
                quality, TestShipments.SHIPMENT_ID, "Süprem", BigDecimal.TEN, ROLLS, "KIR-1", receivedAt, null);
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
    @DisplayName("Faturası kesilen iplik tüketilmiş sayılır: gelen kumaş ve faturalanan kilo gönderilen ipliği aşamaz")
    void shouldCountInvoicedKgAsConsumedYarn() {
        settlements.givenInvoiced("300");

        assertThatThrownBy(() -> service.recordFabricReceipt(command("700.001", null)))
                .isInstanceOf(FabricExceedsSentYarnException.class);
        assertThat(ledger.receipts()).isEmpty();
        assertThat(service.recordFabricReceipt(command("700", null)).netKg()).isEqualByComparingTo("700");
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
                clock);

        // when / then
        assertThatThrownBy(() -> withoutFile.recordFabricReceipt(command("10", null)))
                .isInstanceOf(FireSettlementNotFoundException.class);
    }
}
