package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchCommand;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.InvalidWeightDecisionException;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResolveWeightMismatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");
    private static final int SCALE = 3;

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<String> calls = new ArrayList<>();
    private final List<CancelShipmentCommand> cancels = new ArrayList<>();
    private final List<RecipeLine> acceptedLines = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor manager = TestTokens.actor("manager");

    private final WeightDecisionPort port = new WeightDecisionPort() {
        @Override
        public void recordDecision(UUID shipmentId, WeightDecision decision, String reason, Audit audit) {
            calls.add("decision:" + decision + ":" + reason);
        }

        @Override
        public void backToDraft(UUID shipmentId, String reason, Audit audit) {
            calls.add("draft");
        }

        @Override
        public void acceptRecipe(RecipeAcceptance acceptance, String reason, Audit audit) {
            BigDecimal measuredKg = acceptance.measuredKg();
            ShipmentDispatched event = acceptance.event();
            acceptedLines.addAll(acceptance.scaledLines());
            calls.add("accept:" + measuredKg.setScale(SCALE).toPlainString() + ":"
                    + event.netKg().setScale(SCALE).toPlainString());
        }
    };

    private final ResolveWeightMismatchService service = new ResolveWeightMismatchService(
            ServiceTestSupport.loader(queryPort, clock), port, cancel -> cancels.add(cancel), clock);

    private ResolveWeightMismatchCommand command(WeightDecision decision, String reason) {
        return new ResolveWeightMismatchCommand(manager, TestShipments.SHIPMENT_ID, decision, reason);
    }

    @Test
    @DisplayName("İptal kararı gerekçeyle iptal akışını çalıştırır")
    void shouldCancelThroughCancellationFlow() {
        queryPort.add(RecipeFixtures.mismatched());

        service.resolve(command(WeightDecision.CANCEL, "Yanlış yükleme"));

        assertThat(calls).containsExactly("decision:CANCEL:Yanlış yükleme");
        assertThat(cancels)
                .singleElement()
                .satisfies(cancel -> assertThat(cancel.reason()).isEqualTo("Yanlış yükleme"));
    }

    @Test
    @DisplayName("Çuval ekle/çıkar kararı sevkiyatı taslağa döndürür")
    void shouldSendBackToDraft() {
        queryPort.add(RecipeFixtures.mismatched());

        service.resolve(command(WeightDecision.ADD_REMOVE_BAGS, "Çuval eksik"));

        assertThat(calls).containsExactly("decision:ADD_REMOVE_BAGS:Çuval eksik", "draft");
        assertThat(cancels).isEmpty();
    }

    @Test
    @DisplayName("Reçeteyi güncelle kararı satırları tartılan net'e eşitler ve sevkiyatı yola çıkarır")
    void shouldScaleRecipeToWeighedNet() {
        queryPort.add(RecipeFixtures.mismatched());

        service.resolve(command(WeightDecision.UPDATE_RECIPE, "Fark kabul edildi"));

        assertThat(calls).last().isEqualTo("accept:1982.500:1982.500");
        assertThat(acceptedLines.stream()
                        .map(RecipeLine::netKg)
                        .reduce(BigDecimal::add)
                        .orElseThrow())
                .isEqualByComparingTo("1982.500");
    }

    @Test
    @DisplayName("Gerekçesiz karar reddedilir ve hiçbir şey yazılmaz")
    void shouldRequireReason() {
        queryPort.add(RecipeFixtures.mismatched());

        assertThatThrownBy(() -> service.resolve(command(WeightDecision.UPDATE_RECIPE, "  ")))
                .isInstanceOf(InvalidWeightDecisionException.class);
        assertThat(calls).isEmpty();
    }

    @Test
    @DisplayName("Uyuşmazlıkta olmayan sevkiyata karar verilemez")
    void shouldRejectDecisionOutsideMismatch() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        assertThatThrownBy(() -> service.resolve(command(WeightDecision.CANCEL, "x")))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(calls).isEmpty();
    }
}
