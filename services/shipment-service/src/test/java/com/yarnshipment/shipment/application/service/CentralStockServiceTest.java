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
import com.yarnshipment.shipment.domain.fabric.YarnShare;
import com.yarnshipment.shipment.domain.stock.CentralProduction;
import com.yarnshipment.shipment.domain.stock.CentralStockMovement;
import com.yarnshipment.shipment.domain.stock.InsufficientCentralStockException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CentralStockServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T09:00:00Z");

    private final InMemoryStock stock = new InMemoryStock();
    private final Actor quality = TestTokens.actor("quality");
    private final RecordingLedgerEvents events = new RecordingLedgerEvents();
    private final CentralStockService service =
            new CentralStockService(stock, events, Clock.fixed(NOW, ZoneOffset.UTC));

    private RecordCentralProductionUseCase.Command production(String yarnType, String fabricKg) {
        List<YarnShare> yarns = List.of(new YarnShare(yarnType, new BigDecimal(fabricKg)));
        return new RecordCentralProductionUseCase.Command(
                quality, "Süprem", new BigDecimal("0.0500"), null, null, yarns);
    }

    private void receive(String yarnType, String netKg) {
        service.recordYarnReceipt(new RecordYarnReceiptUseCase.Command(quality, yarnType, new BigDecimal(netKg), null));
    }

    @Test
    @DisplayName("Stokta girişi olan iplikten yetecek kadar üretim yapılır ve iplik fire ile düşer")
    void shouldProduceFromAvailableStock() {
        receive("Pamuk 20/1", "100");

        service.recordCentralProduction(production("Pamuk 20/1", "95"));

        assertThat(stock.stockOf("Pamuk 20/1")).isEqualByComparingTo("0");
        assertThat(events.names).containsExactly("yarnReceipt", "centralProduction");
    }

    @Test
    @DisplayName("Stokta hiç girişi olmayan iplikle üretim reddedilir")
    void shouldRejectProductionWithoutStock() {
        assertThatThrownBy(() -> service.recordCentralProduction(production("YOKİPLİK", "5")))
                .isInstanceOf(InsufficientCentralStockException.class);
        assertThat(stock.productions).isEmpty();
        assertThat(events.names).isEmpty();
    }

    @Test
    @DisplayName("Stok yetmiyorsa üretim reddedilir ve hiçbir hareket yazılmaz")
    void shouldRejectProductionExceedingStock() {
        receive("Pamuk 20/1", "10");

        assertThatThrownBy(() -> service.recordCentralProduction(production("Pamuk 20/1", "50")))
                .isInstanceOf(InsufficientCentralStockException.class);
        assertThat(stock.productions).isEmpty();
        assertThat(stock.stockOf("Pamuk 20/1")).isEqualByComparingTo("10");
    }

    /** Yalnızca bu testin ihtiyaç duyduğu bellek içi merkez stok defteri. */
    private static final class InMemoryStock implements CentralStockPort {
        private final Map<String, BigDecimal> kg = new LinkedHashMap<>();
        private final List<CentralProduction> productions = new ArrayList<>();

        BigDecimal stockOf(String yarnType) {
            return kg.getOrDefault(yarnType, BigDecimal.ZERO);
        }

        @Override
        public void saveReceipt(CentralStockMovement receipt) {
            kg.merge(receipt.yarnType(), receipt.netKg(), BigDecimal::add);
        }

        @Override
        public void saveProduction(CentralProduction production, List<CentralStockMovement> consumptions) {
            productions.add(production);
            consumptions.forEach(movement -> kg.merge(movement.yarnType(), movement.netKg(), BigDecimal::add));
        }

        @Override
        public List<CentralStockRow> findStock() {
            return kg.entrySet().stream()
                    .map(entry -> new CentralStockRow(entry.getKey(), entry.getValue()))
                    .toList();
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
