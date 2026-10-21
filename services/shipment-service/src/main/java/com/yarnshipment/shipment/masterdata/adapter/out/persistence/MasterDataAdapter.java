package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataQueryPort;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link MasterDataQueryPort}'un JDBC uygulaması. Arama metni LIKE kaçışından geçirilir; yalnızca etkin kayıtlar ada
 * göre sıralı okunur. Her sorguya tüm adlandırılmış parametreler bağlanır, kullanılmayanlar yok sayılır.
 */
@Component
final class MasterDataAdapter implements MasterDataQueryPort {

    private static final String MATCH = " AND (CAST(:pattern AS text) IS NULL OR %s ILIKE :pattern ESCAPE '\\')";
    private static final String SUBCONTRACTORS = "FROM md_subcontractor WHERE active" + MATCH.formatted("title");
    private static final String DRIVERS = "FROM md_driver WHERE active" + MATCH.formatted("full_name")
            + " AND (CAST(:subcontractorId AS uuid) IS NULL OR subcontractor_id = CAST(:subcontractorId AS uuid))";
    private static final String VEHICLES = "FROM md_vehicle WHERE active" + MATCH.formatted("plate_no");
    private static final String SITES =
            "FROM md_site WHERE active" + MATCH.formatted("name") + " AND (:allSites OR id IN (:siteIds))";
    private final PagedQuery paged;

    MasterDataAdapter(JdbcClient jdbc) {
        this.paged = new PagedQuery(jdbc);
    }

    @Override
    public MasterDataPage<Subcontractor> findSubcontractors(MasterDataQuery query) {
        return paged.page(
                new PagedQuery.Select(SUBCONTRACTORS, "title", "id, title"),
                query,
                new PagedQuery.Filters(null, true, PagedQuery.NO_SITES),
                rs -> new Subcontractor(rs.getObject("id", UUID.class), rs.getString("title")));
    }

    @Override
    public MasterDataPage<Driver> findDrivers(MasterDataQuery query, @Nullable UUID subcontractorId) {
        return paged.page(
                new PagedQuery.Select(DRIVERS, "full_name", "id, full_name, subcontractor_id"),
                query,
                new PagedQuery.Filters(subcontractorId, true, PagedQuery.NO_SITES),
                rs -> new Driver(
                        rs.getObject("id", UUID.class),
                        rs.getString("full_name"),
                        rs.getObject("subcontractor_id", UUID.class)));
    }

    @Override
    public MasterDataPage<Vehicle> findVehicles(MasterDataQuery query) {
        return paged.page(
                new PagedQuery.Select(VEHICLES, "plate_no", "id, plate_no"),
                query,
                new PagedQuery.Filters(null, true, PagedQuery.NO_SITES),
                rs -> new Vehicle(rs.getObject("id", UUID.class), rs.getString("plate_no")));
    }

    @Override
    public MasterDataPage<Site> findSites(AccessScope scope, MasterDataQuery query) {
        if (!scope.allSites() && scope.siteIds().isEmpty()) {
            return paged.empty(query);
        }
        List<UUID> siteIds = scope.siteIds().isEmpty() ? PagedQuery.NO_SITES : List.copyOf(scope.siteIds());
        return paged.page(
                new PagedQuery.Select(SITES, "name", "id, name"),
                query,
                new PagedQuery.Filters(null, scope.allSites(), siteIds),
                rs -> new Site(rs.getObject("id", UUID.class), rs.getString("name")));
    }
}
