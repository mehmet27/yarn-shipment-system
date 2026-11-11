package com.yarnshipment.shipment.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ShipmentSettingsPort;
import com.yarnshipment.shipment.domain.model.ShipmentSettings;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** {@link ShipmentSettingsPort}'un JDBC uygulaması; ayarlar {@code app_setting} anahtar-değer tablosundadır. */
@Component
final class ShipmentSettingsAdapter implements ShipmentSettingsPort {

    static final String FIRE_RATE_KEY = "default_fire_rate";
    static final String TOLERANCE_KEY = "weight_tolerance_kg";
    private static final String SET_ACTOR = "SELECT set_config('app.actor_id', ?, true)";
    private static final String SELECT = "SELECT key, value FROM app_setting";
    private static final String UPSERT = """
            INSERT INTO app_setting (key, value, updated_at, updated_by) VALUES (:key, :value, :at, :actor)
            ON CONFLICT (key) DO UPDATE
                SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by
            WHERE app_setting.value IS DISTINCT FROM EXCLUDED.value""";

    private final JdbcClient jdbc;
    private final Clock clock;

    ShipmentSettingsAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public ShipmentSettings find() {
        Map<String, String> values =
                jdbc
                        .sql(SELECT)
                        .query((rs, row) -> Map.entry(rs.getString("key"), rs.getString("value")))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ShipmentSettings defaults = ShipmentSettings.DEFAULTS;
        return new ShipmentSettings(
                number(values, FIRE_RATE_KEY, defaults.defaultFireRate()),
                number(values, TOLERANCE_KEY, defaults.weightToleranceKg()));
    }

    @Override
    public void save(ShipmentSettings settings, UUID actorUserId) {
        jdbc.sql(SET_ACTOR).param(actorUserId.toString()).query().singleValue();
        upsert(FIRE_RATE_KEY, settings.defaultFireRate().toPlainString(), actorUserId);
        upsert(TOLERANCE_KEY, settings.weightToleranceKg().toPlainString(), actorUserId);
    }

    private void upsert(String key, String value, UUID actor) {
        jdbc.sql(UPSERT)
                .param("key", key)
                .param("value", value)
                .param("at", Timestamp.from(clock.instant()))
                .param("actor", actor)
                .update();
    }

    private static BigDecimal number(Map<String, String> values, String key, BigDecimal fallback) {
        String raw = values.get(key);
        return raw == null ? fallback : new BigDecimal(raw);
    }
}
