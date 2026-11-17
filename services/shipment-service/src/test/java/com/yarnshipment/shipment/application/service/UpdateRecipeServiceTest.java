package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.weight.UpdateRecipeCommand;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.application.port.out.WeightRecipePort;
import com.yarnshipment.shipment.application.service.weight.UpdateRecipeService;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.InvalidWeightDecisionException;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import com.yarnshipment.shipment.domain.weight.RecipeWeightMismatchException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Reçete düzeltme: toplam tartılan net'i (1982.500 kg) tolerans içinde tutuyorsa sevkiyat yola çıkar. */
class UpdateRecipeServiceTest {

    private static final int ADDED_LINE_COUNT = 3;
    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final StubSettingsPort settings = new StubSettingsPort();
    private final List<String> decisions = new ArrayList<>();
    private final List<WeightRecipePort.RecipeReplacement> replacements = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final WeightDecisionPort decisionPort = new WeightDecisionPort() {
        @Override
        public void recordDecision(UUID shipmentId, WeightDecision decision, String reason, Audit audit) {
            decisions.add(decision + ":" + reason);
        }

        @Override
        public void backToDraft(UUID shipmentId, String reason, Audit audit) {
            throw new IllegalStateException("not used");
        }
    };

    private final WeightRecipePort recipePort = (replacement, reason, audit) -> replacements.add(replacement);

    private final UpdateRecipeService service = new UpdateRecipeService(
            ServiceTestSupport.loader(queryPort, clock), settings, decisionPort, recipePort, clock);

    private static RecipeLine line(int no, String kg) {
        return RecipeFixtures.recipeLine(no, null, "İPLİK " + no, ServiceTestSupport.decimal(kg));
    }

    private UpdateRecipeCommand command(String reason, RecipeLine... lines) {
        return new UpdateRecipeCommand(TestTokens.actor("manager"), TestShipments.SHIPMENT_ID, List.of(lines), reason);
    }

    @Test
    @DisplayName("Reçete toplamı tartıyı tutuyorsa reçete yazılır ve sevkiyat yola çıkar")
    void shouldDispatchWhenRecipeMatchesWeighing() {
        queryPort.add(RecipeFixtures.mismatched());

        service.updateRecipe(command("Reçete düzeltildi", line(1, "1000.000"), line(2, "982.500")));

        assertThat(decisions).containsExactly("UPDATE_RECIPE:Reçete düzeltildi");
        assertThat(replacements).singleElement().satisfies(replacement -> {
            assertThat(replacement.totalKg()).isEqualByComparingTo("1982.500");
            assertThat(replacement.event().netKg()).isEqualByComparingTo("1982.500");
            assertThat(replacement.lines()).hasSize(2);
        });
    }

    @Test
    @DisplayName("Fark toleransın içindeyse yola çıkar; dışındaysa hiçbir şey yazılmaz")
    void shouldCheckAgainstTolerance() {
        queryPort.add(RecipeFixtures.mismatched());
        ServiceTestSupport.saveTolerance(settings, "10.000");

        service.updateRecipe(command("Tolerans içinde", line(1, "1000.000"), line(2, "990.000")));
        assertThat(replacements).hasSize(1);

        assertThatThrownBy(() ->
                        service.updateRecipe(command("Tolerans dışında", line(1, "1000.000"), line(2, "993.000"))))
                .isInstanceOfSatisfying(RecipeWeightMismatchException.class, exception -> {
                    assertThat(exception.recipeKg()).isEqualByComparingTo("1993.000");
                    assertThat(exception.measuredKg()).isEqualByComparingTo("1982.500");
                    assertThat(exception.toleranceKg()).isEqualByComparingTo("10.000");
                });
        assertThat(replacements).hasSize(1);
        assertThat(decisions).hasSize(1);
    }

    @Test
    @DisplayName("Reçete tartıyı tutmuyorsa (kilo aynı kalmış) reddedilir; karar da kaydedilmez")
    void shouldRejectUnchangedMismatchedRecipe() {
        queryPort.add(RecipeFixtures.mismatched());
        ServiceTestSupport.saveTolerance(settings, "1.000");

        assertThatThrownBy(() -> service.updateRecipe(command("Aynı", line(1, "1000.000"), line(2, "900.000"))))
                .isInstanceOf(RecipeWeightMismatchException.class);
        assertThat(decisions).isEmpty();
        assertThat(replacements).isEmpty();
    }

    @Test
    @DisplayName("Satır silinemez; yeni satır eklenebilir")
    void shouldNotRemoveButMayAddLines() {
        queryPort.add(RecipeFixtures.mismatched());

        assertThatThrownBy(() -> service.updateRecipe(command("Eksik satır", line(1, "1982.500"))))
                .isInstanceOf(InvalidWeightDecisionException.class);

        service.updateRecipe(
                command("Satır eklendi", line(1, "1000.000"), line(2, "500.000"), line(ADDED_LINE_COUNT, "482.500")));
        assertThat(replacements)
                .singleElement()
                .satisfies(replacement -> assertThat(replacement.lines()).hasSize(ADDED_LINE_COUNT));
    }

    @Test
    @DisplayName("Gerekçesiz istek reddedilir")
    void shouldRequireReason() {
        queryPort.add(RecipeFixtures.mismatched());

        assertThatThrownBy(() -> service.updateRecipe(command("  ", line(1, "1000.000"), line(2, "982.500"))))
                .isInstanceOf(InvalidWeightDecisionException.class);
        assertThat(replacements).isEmpty();
    }

    @Test
    @DisplayName("Uyuşmazlıkta olmayan sevkiyatın reçetesi bu uçtan değişmez")
    void shouldRejectOutsideMismatch() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        assertThatThrownBy(() -> service.updateRecipe(command("x", line(1, "1982.500"))))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(replacements).isEmpty();
    }
}
