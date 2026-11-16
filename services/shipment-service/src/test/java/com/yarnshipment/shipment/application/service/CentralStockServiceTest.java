package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.stock.CentralStockRow;
import com.yarnshipment.shipment.application.port.in.stock.RecordCentralProductionUseCase;
import com.yarnshipment.shipment.application.port.in.stock.RecordYarnReceiptUseCase;
import com.yarnshipment.shipment.application.port.in.stock.YarnFlow;
import com.yarnshipment.shipment.application.port.out.ledger.CentralStockPort;
import com.yarnshipment.shipment.domain.balance.InvalidLedgerInputException;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CentralStockServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");

    private static final UUID PAMUK = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID PASIF = UUID.fromString("0192f0a0-0000-7000-8000-00000000a002");
    private static final UUID STOKSUZ = UUID.fromString("0192f0a0-0000-7000-8000-00000000a004");
    private static final UUID YOK = UUID.fromString("0192f0a0-0000-7000-8000-00000000a003");

    private final InMemoryStock stock = new InMemoryStock();
    private final Actor quality = TestTokens.actor("quality");
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final CentralStockService service =
            new CentralStockService(stock, events, Clock.fixed(NOW, ZoneOffset.UTC));

    private RecordCentralProductionUseCase.Command production(UUID product, String fabricKg) {
        List<RecordCentralProductionUseCase.YarnRequest> yarns =
                List.of(new RecordCentralProductionUseCase.YarnRequest(product, new BigDecimal(fabricKg)));
        return new RecordCentralProductionUseCase.Command(
                quality, "Süprem", new BigDecimal("0.0500"), null, null, yarns);
    }

    private void receive(UUID product, String netKg) {
        service.recordYarnReceipt(new RecordYarnReceiptUseCase.Command(quality, product, new BigDecimal(netKg), null));
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

    /** Yalnızca bu testin ihtiyaç duyduğu bellek içi merkez stok defteri. */
    private static final class InMemoryStock implements CentralStockPort {
        private final Map<UUID, BigDecimal> kg = new LinkedHashMap<>();
        private final Map<UUID, String> names = new HashMap<>();
        private final List<CentralProduction> productions = new ArrayList<>();

        BigDecimal stockOf(UUID product) {
            return kg.getOrDefault(product, BigDecimal.ZERO);
        }

        @Override
        public void saveReceipt(CentralStockMovement receipt) {
            names.put(receipt.productId(), receipt.yarnType());
            kg.merge(receipt.productId(), receipt.netKg(), BigDecimal::add);
        }

        @Override
        public void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions) {
            productions.add(production);
            consumptions.forEach(movement -> kg.merge(movement.productId(), movement.netKg(), BigDecimal::add));
        }

        @Override
        public List<CentralStockRow> findStock() {
            return kg.entrySet().stream()
                    .map(entry -> new CentralStockRow(
                            entry.getKey(), names.getOrDefault(entry.getKey(), ""), entry.getValue()))
                    .toList();
        }

        @Override
        public Map<UUID, String> findActiveProductNames(Collection<UUID> productIds) {
            Map<UUID, String> active = Map.of(PAMUK, "Pamuk 20/1", STOKSUZ, "Stoksuz İplik");
            Map<UUID, String> found = new HashMap<>();
            productIds.stream().filter(active::containsKey).forEach(id -> found.put(id, active.get(id)));
            return found;
        }

        @Override
        public YarnFlow findYarnFlow() {
            return new YarnFlow(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        @Override
        public List<CentralStockMovement> findRecent(int limit) {
            return List.of();
        }

        @Override
        public BigDecimal findProducedFabricKg() {
            return BigDecimal.ZERO;
        }
    }
}
