package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.BrandingPort;
import com.yarnshipment.shipment.masterdata.domain.branding.Branding;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@link BrandingPort}'un JDBC uygulaması; marka {@code app_setting} tablosunda üç anahtar olarak durur
 * ({@code branding_*}; kaldırılan değer boş metindir), iz {@code audit_local} trigger'ından düşer.
 * Ana renk satırı yoksa marka tanımsızdır.
 */
@Component
final class BrandingAdapter implements BrandingPort {

    static final String NAME_KEY = "branding_company_name";
    static final String COLOR_KEY = "branding_primary_color";
    static final String LOGO_KEY = "branding_logo";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String SELECT = "SELECT key, value FROM app_setting WHERE key LIKE 'branding\\_%'";
    private static final String UPSERT = """
            INSERT INTO app_setting (key, value, updated_at, updated_by) VALUES (:key, :value, :at, :actor)
            ON CONFLICT (key) DO UPDATE
                SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by
            WHERE app_setting.value IS DISTINCT FROM EXCLUDED.value""";

    private final JdbcClient jdbc;
    private final Clock clock;

    BrandingAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Optional<Branding> findBranding() {
        Map<String, String> values =
                jdbc
                        .sql(SELECT)
                        .query((rs, row) -> Map.entry(rs.getString("key"), rs.getString("value")))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String color = values.get(COLOR_KEY);
        if (color == null) {
            return Optional.empty();
        }
        return Optional.of(new Branding(blankToNull(values.get(NAME_KEY)), color, blankToNull(values.get(LOGO_KEY))));
    }

    @Override
    public Branding upsertBranding(UUID actorId, Branding branding) {
        jdbc.sql(SET_ACTOR).param(actorId.toString()).query().singleValue();
        upsert(NAME_KEY, branding.companyName(), actorId);
        upsert(COLOR_KEY, branding.primaryColor(), actorId);
        upsert(LOGO_KEY, branding.logoDataUrl(), actorId);
        return findBranding().orElse(branding);
    }

    private void upsert(String key, @Nullable String value, UUID actorId) {
        jdbc.sql(UPSERT)
                .param("key", key)
                .param("value", value == null ? "" : value)
                .param("at", Timestamp.from(clock.instant()))
                .param("actor", actorId)
                .update();
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
