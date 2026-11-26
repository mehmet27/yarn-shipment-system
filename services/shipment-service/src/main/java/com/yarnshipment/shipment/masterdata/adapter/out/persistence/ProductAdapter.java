package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.ProductPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link ProductPort}'un JDBC uygulaması. Kod tekilliği büyük/küçük harf duyarsız benzersiz indeksle korunur; iz
 * {@code audit_local} trigger'ından düşer.
 */
@Component
final class ProductAdapter implements ProductPort {

    private static final String COLUMNS = "id, code, name, yarn_count, active";
    private static final String LIST = "FROM md_product WHERE (active OR :includeInactive)"
            + " AND (CAST(:pattern AS text) IS NULL OR code ILIKE :pattern ESCAPE '\\'"
            + " OR name ILIKE :pattern ESCAPE '\\')";
    private static final String INSERT = """
            INSERT INTO md_product (id, code, name, yarn_count, created_at, created_by)
            VALUES (:id, :code, :name, :extra, :at, :actor)""";
    private static final String UPDATE =
            "UPDATE md_product SET name = :name, yarn_count = :extra WHERE id = :id RETURNING " + COLUMNS;
    private static final String STATUS = "UPDATE md_product SET active = :active WHERE id = :id";
    /** Karışımlar ipliği adıyla tutar; ad değişince aynı işlemde karışım satırları da yeni ada geçer. */
    private static final String RENAME_IN_RECIPES = """
            UPDATE md_fabric_recipe r
            SET lines = (SELECT jsonb_agg(CASE WHEN lower(btrim(e.line ->> 'yarnType')) = lower(btrim(p.name))
                                              THEN jsonb_set(e.line, '{yarnType}', to_jsonb(CAST(:name AS text)))
                                              ELSE e.line END ORDER BY e.position)
                         FROM jsonb_array_elements(r.lines) WITH ORDINALITY AS e(line, position)),
                updated_at = :at
            FROM md_product p
            WHERE p.id = :id AND p.name <> :name
              AND EXISTS (SELECT 1 FROM jsonb_array_elements(r.lines) AS x(line)
                          WHERE lower(btrim(x.line ->> 'yarnType')) = lower(btrim(p.name)))""";

    private static final String NAME_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM md_product
                           WHERE lower(btrim(name)) = lower(btrim(:name)) AND id IS DISTINCT FROM :id)""";

    private final PagedQuery paged;
    private final CatalogWriter writer;
    private final JdbcClient jdbc;
    private final Clock clock;

    ProductAdapter(JdbcClient jdbc, Clock clock) {
        this.paged = new PagedQuery(jdbc);
        this.writer = new CatalogWriter(jdbc, clock);
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public MasterDataPage<Product> findProducts(MasterDataQuery query) {
        return paged.page(
                new PagedQuery.Select(LIST, "code", COLUMNS),
                query,
                new PagedQuery.Filters(null, true, PagedQuery.NO_SITES),
                ProductAdapter::product);
    }

    @Override
    public Product insertProduct(UUID actorId, String code, ProductDetails details) {
        writer.requireFreeName(NAME_TAKEN, details.name(), null);
        UUID id = writer.insert(INSERT, actorId, row(code, details));
        return new Product(id, code, details, true);
    }

    @Override
    public Optional<Product> updateProduct(UUID actorId, UUID id, ProductDetails details) {
        writer.requireFreeName(NAME_TAKEN, details.name(), id);
        jdbc.sql(RENAME_IN_RECIPES)
                .param("id", id)
                .param("name", details.name())
                .param("at", Timestamp.from(clock.instant()))
                .update();
        return writer.rewrite(UPDATE, actorId, id, row("", details))
                .query((rs, rowNumber) -> product(rs))
                .optional();
    }

    @Override
    public boolean setProductStatus(UUID actorId, UUID id, MasterDataStatus status) {
        return writer.setStatus(STATUS, actorId, id, status);
    }

    private static CatalogWriter.Row row(String code, ProductDetails details) {
        return new CatalogWriter.Row(code, details.name(), details.yarnCount());
    }

    private static Product product(ResultSet rs) throws SQLException {
        return new Product(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                new ProductDetails(rs.getString("name"), rs.getString("yarn_count")),
                rs.getBoolean("active"));
    }
}
