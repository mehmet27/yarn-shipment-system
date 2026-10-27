package com.yarnshipment.discrepancy.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyActivityView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyPage;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyType;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyView;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.PageMetadata;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.Resolution;
import com.yarnshipment.discrepancy.adapter.in.web.api.model.Severity;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Domain modelini web modeline çevirir. Kilo ve yüzde değerleri ondalık dize olarak taşınır (API-03). */
final class DiscrepancyViews {

    private DiscrepancyViews() {}

    static DiscrepancyPage toPage(com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage page, Actor actor) {
        PageMetadata metadata = new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages());
        List<DiscrepancyView> items =
                page.items().stream().map(item -> toView(item, actor)).toList();
        return new DiscrepancyPage(items, metadata);
    }

    static ListDiscrepanciesQuery.Filter filterOf(
            com.yarnshipment.discrepancy.adapter.in.web.api.model.@Nullable DiscrepancyStatus status,
            @Nullable Severity severity,
            @Nullable UUID assigneeId,
            @Nullable UUID shipmentId) {
        return new ListDiscrepanciesQuery.Filter(
                status == null ? null : DiscrepancyStatus.valueOf(status.getValue()),
                severity == null
                        ? null
                        : com.yarnshipment.discrepancy.domain.model.Severity.valueOf(severity.getValue()),
                assigneeId,
                shipmentId);
    }

    static com.yarnshipment.discrepancy.domain.model.Resolution resolutionOf(Resolution resolution) {
        return com.yarnshipment.discrepancy.domain.model.Resolution.valueOf(resolution.getValue());
    }

    static DiscrepancyView toView(Discrepancy discrepancy, Actor actor) {
        Discrepancy.Decision decision = discrepancy.decision();
        DiscrepancyView view = new DiscrepancyView(
                discrepancy.id(),
                discrepancy.discrepancyNo(),
                discrepancy.siteId(),
                discrepancy.shipmentId(),
                DiscrepancyType.fromValue(discrepancy.basis().type().name()),
                Severity.fromValue(discrepancy.basis().severity().name()),
                com.yarnshipment.discrepancy.adapter.in.web.api.model.DiscrepancyStatus.fromValue(
                        discrepancy.status().name()),
                discrepancy.basis().ruleCode(),
                requiredUtc(discrepancy.createdAt()));
        view.expectedValue(discrepancy.measure().expected())
                .actualValue(discrepancy.measure().actual())
                .diffKg(plain(discrepancy.measure().diffKg()))
                .diffPct(plain(discrepancy.measure().diffPct()))
                .assigneeId(discrepancy.handling().assigneeId())
                .dueAt(utc(discrepancy.handling().dueAt()))
                .allowedActions(allowedActions(discrepancy, actor));
        if (decision != null) {
            view.resolution(Resolution.fromValue(decision.resolution().name()))
                    .resolutionNote(decision.note())
                    .liableParty(decision.liableParty())
                    .resolvedBy(decision.resolvedBy())
                    .resolvedAt(utc(decision.resolvedAt()));
        }
        return view;
    }

    static DiscrepancyActivityView toView(DiscrepancyActivity activity) {
        return new DiscrepancyActivityView(
                        activity.id(), activity.actionType(), activity.actorId(), requiredUtc(activity.occurredAt()))
                .note(activity.note())
                .evidenceId(activity.evidenceId());
    }

    /** FE-SEC-03: butonlar yalnızca bu listeye göre gösterilir. */
    private static List<DiscrepancyView.AllowedActionsEnum> allowedActions(Discrepancy discrepancy, Actor actor) {
        if (!discrepancy.status().isUnresolved() || !actor.hasPermission(Permission.DISCREPANCY_RESOLVE)) {
            return List.of();
        }
        return List.of(DiscrepancyView.AllowedActionsEnum.ASSIGN, DiscrepancyView.AllowedActionsEnum.RESOLVE);
    }

    private static @Nullable String plain(@Nullable BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static @Nullable OffsetDateTime utc(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime requiredUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
