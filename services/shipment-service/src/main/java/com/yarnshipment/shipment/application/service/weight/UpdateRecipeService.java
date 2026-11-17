package com.yarnshipment.shipment.application.service.weight;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.shipment.application.port.in.weight.UpdateRecipeCommand;
import com.yarnshipment.shipment.application.port.in.weight.UpdateRecipeUseCase;
import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.application.port.out.WeightDecisionPort;
import com.yarnshipment.shipment.application.port.out.WeightRecipePort;
import com.yarnshipment.shipment.application.service.ScopedShipmentLoader;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.InvalidWeightDecisionException;
import com.yarnshipment.shipment.domain.model.Recipe;
import com.yarnshipment.shipment.domain.model.RecipeLine;
import com.yarnshipment.shipment.domain.model.Shipment;
import com.yarnshipment.shipment.domain.model.ShipmentDispatched;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.model.Weighing;
import com.yarnshipment.shipment.domain.model.WeightDecision;
import com.yarnshipment.shipment.domain.weight.RecipeWeightMismatchException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kantar uyuşmazlığındaki sevkiyatın reçetesini müdür düzeltir. Yeni satırların toplamı tartılan net'e tolerans
 * içinde uyuyorsa reçete kaydedilir ve sevkiyat yola çıkar; uymuyorsa hiçbir şey kaydedilmez. Satır silinemez
 * (kayıtlar değiştirilemez), yalnızca düzeltilir ya da yenisi eklenir.
 */
@Service
public class UpdateRecipeService implements UpdateRecipeUseCase {

    private static final int MAX_REASON_LENGTH = 500;

    private final ScopedShipmentLoader shipments;
    private final ShipmentSettingsPort settings;
    private final WeightDecisionPort decisionPort;
    private final WeightRecipePort recipePort;
    private final Clock clock;

    public UpdateRecipeService(
            ScopedShipmentLoader shipments,
            ShipmentSettingsPort settings,
            WeightDecisionPort decisionPort,
            WeightRecipePort recipePort,
            Clock clock) {
        this.shipments = shipments;
        this.settings = settings;
        this.decisionPort = decisionPort;
        this.recipePort = recipePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void updateRecipe(UpdateRecipeCommand command) {
        Actor actor = command.actor();
        Shipment shipment = shipments.load(actor, command.shipmentId(), Permission.SHIPMENT_CANCEL);
        String reason = command.reason().strip();
        if (reason.isEmpty() || reason.length() > MAX_REASON_LENGTH) {
            throw new InvalidWeightDecisionException("reason", "required");
        }
        if (shipment.status() != ShipmentStatus.WEIGHT_MISMATCH) {
            throw new InvalidShipmentStatusException(shipment.id(), shipment.status(), ShipmentStatus.DRAFT);
        }
        Weighing weighing = shipment.dispatchWeighing();
        List<RecipeLine> lines = command.lines();
        if (weighing == null || lines.size() < shipment.lines().size()) {
            throw new InvalidWeightDecisionException("lines", "recipe lines cannot be removed");
        }
        BigDecimal measured = weighing.netKg();
        requireFitsWeighing(lines, measured);
        BigDecimal total = Recipe.totalKg(lines);
        WeightDecisionPort.Audit audit = new WeightDecisionPort.Audit(actor.userId(), clock.instant());
        decisionPort.recordDecision(shipment.id(), WeightDecision.UPDATE_RECIPE, reason, audit);
        ShipmentDispatched event = new ShipmentDispatched(
                shipment.id(),
                shipment.siteId(),
                shipment.subcontractorId(),
                weighing.bagCount(),
                measured,
                audit.at());
        recipePort.replaceAndDispatch(
                new WeightRecipePort.RecipeReplacement(shipment.id(), lines, total, event), reason, audit);
    }

    private void requireFitsWeighing(List<RecipeLine> lines, BigDecimal measured) {
        BigDecimal tolerance = settings.find().weightToleranceKg();
        if (!Recipe.fitsWeighing(lines, measured, tolerance)) {
            throw new RecipeWeightMismatchException(Recipe.totalKg(lines), measured, tolerance);
        }
    }
}
