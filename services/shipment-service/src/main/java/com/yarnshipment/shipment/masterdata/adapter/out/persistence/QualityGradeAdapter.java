package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.QualityGradePort;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link QualityGradePort}'un JDBC uygulaması. Kod tekilliği büyük/küçük harf duyarsız benzersiz indeksle korunur; iz
 * {@code audit_local} trigger'ından düşer.
 */
@Component
final class QualityGradeAdapter implements QualityGradePort {

    private static final String COLUMNS = "id, code, name, label_color, active";
    private static final String LIST = "FROM md_quality_grade WHERE (active OR :includeInactive)"
            + " AND (CAST(:pattern AS text) IS NULL OR code ILIKE :pattern ESCAPE '\\'"
            + " OR name ILIKE :pattern ESCAPE '\\')";
    private static final String INSERT = """
            INSERT INTO md_quality_grade (id, code, name, label_color, created_at, created_by)
            VALUES (:id, :code, :name, :extra, :at, :actor)""";
    private static final String UPDATE =
            "UPDATE md_quality_grade SET name = :name, label_color = :extra WHERE id = :id RETURNING " + COLUMNS;
    private static final String STATUS = "UPDATE md_quality_grade SET active = :active WHERE id = :id";

    private final PagedQuery paged;
    private final CatalogWriter writer;

    QualityGradeAdapter(JdbcClient jdbc, Clock clock) {
        this.paged = new PagedQuery(jdbc);
        this.writer = new CatalogWriter(jdbc, clock);
    }

    @Override
    public MasterDataPage<QualityGrade> findQualityGrades(MasterDataQuery query) {
        return paged.page(
                new PagedQuery.Select(LIST, "code", COLUMNS),
                query,
                new PagedQuery.Filters(null, true, PagedQuery.NO_SITES),
                QualityGradeAdapter::qualityGrade);
    }

    @Override
    public QualityGrade insertQualityGrade(UUID actorId, String code, QualityGradeDetails details) {
        UUID id = writer.insert(INSERT, actorId, row(code, details));
        return new QualityGrade(id, code, details, true);
    }

    @Override
    public Optional<QualityGrade> updateQualityGrade(UUID actorId, UUID id, QualityGradeDetails details) {
        return writer.rewrite(UPDATE, actorId, id, row("", details))
                .query((rs, rowNumber) -> qualityGrade(rs))
                .optional();
    }

    @Override
    public boolean setQualityGradeStatus(UUID actorId, UUID id, MasterDataStatus status) {
        return writer.setStatus(STATUS, actorId, id, status);
    }

    private static CatalogWriter.Row row(String code, QualityGradeDetails details) {
        return new CatalogWriter.Row(code, details.name(), details.labelColor());
    }

    private static QualityGrade qualityGrade(ResultSet rs) throws SQLException {
        return new QualityGrade(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                new QualityGradeDetails(rs.getString("name"), rs.getString("label_color")),
                rs.getBoolean("active"));
    }
}
