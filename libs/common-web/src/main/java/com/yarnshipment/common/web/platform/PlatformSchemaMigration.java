package com.yarnshipment.common.web.platform;

import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * {@code platform} şemasını kendi {@code flyway_schema_history} tablosuyla göç ettirir. Modüllerin trigger'ları ve
 * Idempotency-Key kayıtları bu şemaya bağlandığı için her modül şemasından önce çalışmalıdır. Migration dosyaları
 * {@code db/platform} altındadır ({@code db/migration} değil), böylece modül Flyway'leri bunları kendi geçmişine almaz.
 */
public final class PlatformSchemaMigration {

    /** Ortak nesnelerin şeması. */
    public static final String SCHEMA = "platform";

    private static final String LOCATION = "classpath:db/platform";

    private PlatformSchemaMigration() {}

    /**
     * Platform şemasını verilen bağlantıyla göç ettirir.
     *
     * @param placeholders Flyway yer tutucuları; {@code app_role} uygulamanın bağlandığı roldür
     */
    public static void migrate(DataSource dataSource, Map<String, String> placeholders) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .locations(LOCATION)
                .placeholders(placeholders)
                .load()
                .migrate();
    }

    /**
     * Tek modülün dilim testleri için: modül göçünden hemen önce platform şemasını aynı bağlantı ve yer tutucularla
     * göç ettirir. Üretimde yarnshipment-app platform göçünü modül göçlerinden önce kendisi çalıştırır.
     */
    public static void migrate(Flyway moduleFlyway) {
        migrate(
                moduleFlyway.getConfiguration().getDataSource(),
                moduleFlyway.getConfiguration().getPlaceholders());
    }
}
