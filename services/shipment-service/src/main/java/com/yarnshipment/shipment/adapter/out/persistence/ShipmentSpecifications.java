package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.application.port.in.ListShipmentsQuery.Filter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Kapsamı ve liste süzgecini WHERE koşuluna çevirir; kapsam kuralları {@code ScopeGuard.isInScope} ile aynıdır. */
final class ShipmentSpecifications {

    private static final char ESCAPE = '\\';
    private static final String PARTIES = "parties";

    private ShipmentSpecifications() {}

    static Specification<ShipmentJpaEntity> inScopeMatching(AccessScope scope, Filter filter) {
        return (root, query, builder) -> builder.and(predicates(root, builder, scope, filter));
    }

    private static Predicate[] predicates(
            Root<ShipmentJpaEntity> root, CriteriaBuilder builder, AccessScope scope, Filter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (!scope.isUnrestricted()) {
            addScopePredicates(predicates, root, builder, scope);
        }
        if (!filter.statuses().isEmpty()) {
            predicates.add(root.get("status")
                    .in(filter.statuses().stream().map(Enum::name).toList()));
        }
        if (filter.search() != null) {
            predicates.add(matchesText(root, builder, filter.search()));
        }
        if (filter.from() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("shipmentDate"), filter.from()));
        }
        if (filter.to() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("shipmentDate"), filter.to()));
        }
        return predicates.toArray(Predicate[]::new);
    }

    /**
     * Metni sevkiyat numarası, fasoncu adı, şoför adı ve plakada arar. Büyük-küçük harf dönüşümünü iki tarafta da
     * veritabanı yapar; böylece Türkçe harfler (İ, ı) Java ile veritabanı arasında farklı küçülmez (NAME-09).
     */
    private static Predicate matchesText(Root<ShipmentJpaEntity> root, CriteriaBuilder builder, String search) {
        Expression<String> pattern = builder.lower(builder.literal(likePattern(search)));
        return builder.or(
                builder.like(builder.lower(root.get("shipmentNo")), pattern, ESCAPE),
                builder.like(builder.lower(root.get(PARTIES).get("subcontractorTitle")), pattern, ESCAPE),
                builder.like(builder.lower(root.get(PARTIES).get("driverName")), pattern, ESCAPE),
                builder.like(builder.lower(root.get(PARTIES).get("plateNo")), pattern, ESCAPE));
    }

    /** LIKE özel karakterleri (%, _, \) kaçışlanır; kullanıcı girdisi desen olarak yorumlanmaz (PERS-10). */
    private static String likePattern(String search) {
        String escaped = search.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static void addScopePredicates(
            List<Predicate> predicates, Root<ShipmentJpaEntity> root, CriteriaBuilder builder, AccessScope scope) {
        predicates.add(
                scope.siteIds().isEmpty()
                        ? builder.disjunction()
                        : root.get("siteId").in(scope.siteIds()));
    }
}
