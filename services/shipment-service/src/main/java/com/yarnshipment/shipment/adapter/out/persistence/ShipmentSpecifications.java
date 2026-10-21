package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Kapsam ve durum filtresini WHERE koşuluna çevirir; kurallar {@code ScopeGuard.isInScope} ile aynıdır. */
final class ShipmentSpecifications {

    private ShipmentSpecifications() {}

    static Specification<ShipmentJpaEntity> inScopeWithStatus(AccessScope scope, Set<ShipmentStatus> statuses) {
        return (root, query, builder) -> builder.and(predicates(root, builder, scope, statuses));
    }

    private static Predicate[] predicates(
            Root<ShipmentJpaEntity> root, CriteriaBuilder builder, AccessScope scope, Set<ShipmentStatus> statuses) {
        List<Predicate> predicates = new ArrayList<>();
        if (!scope.isUnrestricted()) {
            addScopePredicates(predicates, root, builder, scope);
        }
        if (!statuses.isEmpty()) {
            predicates.add(
                    root.get("status").in(statuses.stream().map(Enum::name).toList()));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static void addScopePredicates(
            List<Predicate> predicates, Root<ShipmentJpaEntity> root, CriteriaBuilder builder, AccessScope scope) {
        predicates.add(
                scope.siteIds().isEmpty()
                        ? builder.disjunction()
                        : root.get("siteId").in(scope.siteIds()));
        UUID subcontractorId = scope.subcontractorId();
        UUID driverId = scope.driverId();
        if (subcontractorId != null) {
            predicates.add(builder.equal(root.get("parties").get("subcontractorId"), subcontractorId));
        } else if (driverId != null) {
            predicates.add(builder.equal(root.get("parties").get("driverId"), driverId));
        }
    }
}
