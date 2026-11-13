package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepancyActivityUseCase;
import com.yarnshipment.discrepancy.application.port.out.ActorNamePort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Uyuşmazlık geçmişi. */
@Service
public class ListDiscrepancyActivityService implements ListDiscrepancyActivityUseCase {

    private static final String ACTION = "DISCREPANCY_VIEW";
    private static final String ASSIGNED = "ASSIGNED";

    private final DiscrepancyQueryPort queryPort;
    private final DiscrepancyAccess access;
    private final ActorNamePort actorNames;

    public ListDiscrepancyActivityService(
            DiscrepancyQueryPort queryPort, ScopeGuard scopeGuard, ActorNamePort actorNames) {
        this.queryPort = queryPort;
        this.actorNames = actorNames;
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyActivity> listActivity(GetDiscrepancyQuery query) {
        access.load(query.actor(), query.discrepancyId(), ACTION);
        List<DiscrepancyActivity> activity = queryPort.findActivity(query.discrepancyId());
        Set<UUID> ids = new HashSet<>();
        activity.forEach(item -> {
            ids.add(item.actorId());
            UUID assignee = assignee(item);
            if (assignee != null) {
                ids.add(assignee);
            }
        });
        Map<UUID, String> names = actorNames.namesOf(ids);
        return activity.stream()
                .map(item -> {
                    UUID assignee = assignee(item);
                    return item.withNames(names.get(item.actorId()), assignee == null ? null : names.get(assignee));
                })
                .toList();
    }

    /** Atama kaydının notu atanan kişinin kimliğidir; başka kayıtta ya da bozuk notta kimlik yoktur. */
    private static @Nullable UUID assignee(DiscrepancyActivity item) {
        if (!ASSIGNED.equals(item.actionType()) || item.note() == null) {
            return null;
        }
        try {
            return UUID.fromString(item.note());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
