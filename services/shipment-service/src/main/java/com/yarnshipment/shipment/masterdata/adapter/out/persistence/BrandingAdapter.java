package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.BrandingPort;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link BrandingPort}'un JDBC uygulaması; tek satır vardır, iz {@code audit_local} trigger'ından düşer. */
@Component
final class BrandingAdapter implements BrandingPort {

    private static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String SELECT = "SELECT company_name, primary_color, logo_data_url FROM md_branding";
    private static final String UPSERT = """
            INSERT INTO md_branding (id, company_name, primary_color, logo_data_url, updated_at, updated_by)
            VALUES (:id, :name, :color, :logo, :at, :actor)
            ON CONFLICT (id) DO UPDATE
                SET company_name = EXCLUDED.company_name, primary_color = EXCLUDED.primary_color,
                    logo_data_url = EXCLUDED.logo_data_url, updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by""";

    private final JdbcClient jdbc;
    private final Clock clock;

    BrandingAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Optional<Branding> findBranding() {
        return jdbc.sql(SELECT)
                .query((rs, rowNumber) -> new Branding(
                        rs.getString("company_name"), rs.getString("primary_color"), rs.getString("logo_data_url")))
                .optional();
    }

    @Override
    public Branding upsertBranding(UUID actorId, Branding branding) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
        jdbc.sql(UPSERT)
                .param("id", SINGLETON_ID)
                .param("name", branding.companyName())
                .param("color", branding.primaryColor())
                .param("logo", branding.logoDataUrl())
                .param("at", Timestamp.from(clock.instant()))
                .param("actor", actorId)
                .update();
        return findBranding().orElse(branding);
    }
}
