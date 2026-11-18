package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.application.service.receipt.YarnReceiptService;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import com.yarnshipment.shipment.domain.stock.LotRequest;
import com.yarnshipment.shipment.domain.stock.ReceiptDetails;
import com.yarnshipment.shipment.domain.stock.ResolvedLot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class CentralStockServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");

    private static final UUID PAMUK = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID PASIF = UUID.fromString("0192f0a0-0000-7000-8000-00000000a002");
    private static final UUID STOKSUZ = UUID.fromString("0192f0a0-0000-7000-8000-00000000a004");
    private static final UUID YOK = UUID.fromString("0192f0a0-0000-7000-8000-00000000a003");

    private static final UUID TEDARIKCI = UUID.fromString("0192f0a0-0000-7000-8000-00000000b001");

    private final InMemoryCentralStock stock =
            new InMemoryCentralStock(Map.of(PAMUK, "Pamuk 20/1", STOKSUZ, "Stoksuz İplik"));
    private final Actor quality = TestTokens.actor("quality");
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final CentralStockService service = new CentralStockService(stock, events, clock);
    private final List<LotRequest> lotRequests = new ArrayList<>();
    private final YarnReceiptService receipts = new YarnReceiptService(
            stock,
            request -> {
                lotRequests.add(request);
                return new ResolvedLot(UUID.randomUUID(), request.lotNo());
            },
            events,
            clock);

    private RecordCentralProductionUseCase.Command production(UUID product, String fabricKg) {
        List<RecordCentralProductionUseCase.YarnRequest> yarns =
                List.of(new RecordCentralProductionUseCase.YarnRequest(product, new BigDecimal(fabricKg)));
        return new RecordCentralProductionUseCase.Command(
                quality, "Süprem", new BigDecimal("0.0500"), null, null, yarns);
    }

    {
        stock.suppliers.put(TEDARIKCI, "Şireci");
    }

    private void receive(UUID product, String netKg) {
        receipts.recordYarnReceipt(new RecordYarnReceiptUseCase.Command(
                quality, product, TEDARIKCI, new BigDecimal(netKg), null, null, null, null));
    }

    @Test
    @DisplayName("Stokta girişi olan iplikten yetecek kadar üretim yapılır ve iplik fire ile düşer")
    void shouldProduceFromAvailableStock() {
        receive(PAMUK, "100");

        service.recordCentralProduction(production(PAMUK, "95"));

        assertThat(stock.stockOf(PAMUK)).isEqualByComparingTo("0");
        assertThat(events.names).containsExactly("yarnReceipt", "centralProduction");
    }

    @Test
    @DisplayName("Stokta hiç girişi olmayan iplikle üretim reddedilir")
    void shouldRejectProductionWithoutStock() {
        assertThatThrownBy(() -> service.recordCentralProduction(production(STOKSUZ, "5")))
                .isInstanceOf(InsufficientCentralStockException.class);
        assertThat(stock.productions).isEmpty();
        assertThat(events.names).isEmpty();
    }

    @Test
    @DisplayName("Stok yetmiyorsa üretim reddedilir ve hiçbir hareket yazılmaz")
    void shouldRejectProductionExceedingStock() {
        receive(PAMUK, "10");

        assertThatThrownBy(() -> service.recordCentralProduction(production(PAMUK, "50")))
                .isInstanceOf(InsufficientCentralStockException.class);
        assertThat(stock.productions).isEmpty();
        assertThat(stock.stockOf(PAMUK)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Tanımlarda olmayan ya da pasif iplikle giriş ve üretim reddedilir; hiçbir şey yazılmaz")
    void shouldRejectUnknownOrInactiveProduct() {
        assertThatThrownBy(() -> receive(PASIF, "10")).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> receive(YOK, "10")).isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> service.recordCentralProduction(production(PASIF, "5")))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(stock.kg).isEmpty();
        assertThat(events.names).isEmpty();
    }

    @Test
    @DisplayName("Giriş hareketi iplik tanımının adını ve id'sini taşır")
    void shouldStoreProductIdAndName() {
        receive(PAMUK, "10");

        assertThat(stock.stockOf(PAMUK)).isEqualByComparingTo("10");
        assertThat(stock.names).containsEntry(PAMUK, "Pamuk 20/1");
    }

    private RecordYarnReceiptUseCase.Command receipt(UUID supplier, @Nullable String lotNo, @Nullable String waybill) {
        return new RecordYarnReceiptUseCase.Command(
                quality, PAMUK, supplier, new BigDecimal("10"), lotNo, UUID.randomUUID(), waybill, null);
    }

    @Test
    @DisplayName("Giriş tedarikçinin adını, normalleşmiş parti numarasını ve irsaliye numarasını yazar")
    void shouldStoreSupplierLotAndWaybill() {
        receipts.recordYarnReceipt(receipt(TEDARIKCI, " p-1 ", " IRS-7 "));

        ReceiptDetails details = stock.receiptDetails.get(0);
        assertThat(details.supplierName()).isEqualTo("Şireci");
        assertThat(details.lotNo()).isEqualTo("P-1");
        assertThat(details.lotId()).isNotNull();
        assertThat(details.waybillNo()).isEqualTo("IRS-7");
        assertThat(lotRequests).singleElement().satisfies(r -> assertThat(r.lotNo()).isEqualTo("P-1"));
    }

    @Test
    @DisplayName("Parti yazılmazsa parti açılmaz; pasif tedarikçi ve geçersiz parti reddedilir")
    void shouldValidateSupplierAndLot() {
        receipts.recordYarnReceipt(receipt(TEDARIKCI, " ", null));
        assertThat(stock.receiptDetails.get(0).lotId()).isNull();
        assertThat(lotRequests).isEmpty();

        UUID pasif = UUID.randomUUID();
        assertThatThrownBy(() -> receipts.recordYarnReceipt(receipt(pasif, null, null)))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThatThrownBy(() -> receipts.recordYarnReceipt(receipt(TEDARIKCI, "ğ*", null)))
                .isInstanceOf(InvalidLedgerInputException.class);
        String longWaybill = "1".repeat(ReceiptDetails.MAX_WAYBILL_LENGTH + 1);
        assertThatThrownBy(() -> receipts.recordYarnReceipt(receipt(TEDARIKCI, null, longWaybill)))
                .isInstanceOf(InvalidLedgerInputException.class);
        assertThat(stock.receiptDetails).hasSize(1);
    }
}
