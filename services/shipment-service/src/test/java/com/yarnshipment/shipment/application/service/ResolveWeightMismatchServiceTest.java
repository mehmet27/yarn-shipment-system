package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.weight.ResolveWeightMismatchCommand;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.InvalidWeightDecisionException;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.Shipment;
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

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<String> calls = new ArrayList<>();
    private final List<CancelShipmentCommand> cancels = new ArrayList<>();
    private final List<RecipeLine> acceptedLines = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor manager = TestTokens.actor("manager");

    private final WeightDecisionPort port = new WeightDecisionPort() {
        @Override
        public void recordDecision(
                UUID shipmentId, WeightDecision decision, String reason, UUID actorUserId, Instant at) {
            calls.add("decision:" + decision + ":" + reason);
        }

        @Override
        public void backToDraft(UUID shipmentId, String reason, UUID actorUserId, Instant at) {
            calls.add("draft");
        }

        @Override
        public void acceptRecipe(
                UUID shipmentId,
                List<RecipeLine> scaledLines,
                BigDecimal measuredKg,
                ShipmentDispatched event,
                String reason,
                UUID actorUserId,
                Instant at) {
            acceptedLines.addAll(scaledLines);
            calls.add("accept:" + measuredKg.setScale(3).toPlainString() + ":" + event.netKg().setScale(3).toPlainString());
        }
    };

    private final ResolveWeightMismatchService service = new ResolveWeightMismatchService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            port,
            cancel -> cancels.add(cancel),
            clock);

    private Shipment mismatched() {
        Shipment base = TestShipments.shipment(ShipmentStatus.WEIGHT_MISMATCH);
        return new Shipment(
                base.id(),
                base.shipmentNo(),
                base.siteId(),
                base.status(),
                base.parties(),
                base.planned(),
                base.dispatchWeighing(),
                null,
                base.createdAt(),
                List.of(
                        new RecipeLine(1, null, "VİSKON", null, new BigDecimal("1000.000")),
                        new RecipeLine(2, null, "POLY", null, new BigDecimal("982.500"))),
                new BigDecimal("0.0400"),
                null);
    }

    private ResolveWeightMismatchCommand command(WeightDecision decision, String reason) {
        return new ResolveWeightMismatchCommand(manager, TestShipments.SHIPMENT_ID, decision, reason);
    }

    @Test
    @DisplayName("İptal kararı gerekçeyle iptal akışını çalıştırır")
    void shouldCancelThroughCancellationFlow() {
        queryPort.add(mismatched());

        service.resolve(command(WeightDecision.CANCEL, "Yanlış yükleme"));

        assertThat(calls).containsExactly("decision:CANCEL:Yanlış yükleme");
        assertThat(cancels).singleElement().satisfies(cancel -> assertThat(cancel.reason()).isEqualTo("Yanlış yükleme"));
    }

    @Test
    @DisplayName("Çuval ekle/çıkar kararı sevkiyatı taslağa döndürür")
    void shouldSendBackToDraft() {
        queryPort.add(mismatched());

        service.resolve(command(WeightDecision.ADD_REMOVE_BAGS, "Çuval eksik"));

        assertThat(calls).containsExactly("decision:ADD_REMOVE_BAGS:Çuval eksik", "draft");
        assertThat(cancels).isEmpty();
    }

    @Test
    @DisplayName("Reçeteyi güncelle kararı satırları tartılan net'e eşitler ve sevkiyatı yola çıkarır")
    void shouldScaleRecipeToWeighedNet() {
        queryPort.add(mismatched());

        service.resolve(command(WeightDecision.UPDATE_RECIPE, "Fark kabul edildi"));

        assertThat(calls).last().isEqualTo("accept:1982.500:1982.500");
        assertThat(acceptedLines.stream().map(RecipeLine::netKg).reduce(BigDecimal::add).orElseThrow())
                .isEqualByComparingTo("1982.500");
    }

    @Test
    @DisplayName("Gerekçesiz karar reddedilir ve hiçbir şey yazılmaz")
    void shouldRequireReason() {
        queryPort.add(mismatched());

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
