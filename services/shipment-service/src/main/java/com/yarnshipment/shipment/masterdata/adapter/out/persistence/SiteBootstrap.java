package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Arayüzde lokasyon tanımlama ekranı yoktur (sevkiyat ve parti tek lokasyonla çalışır), bu yüzden hiç lokasyon yoksa
 * ("Merkez") varsayılan lokasyon açılır ve lokasyonu olmayan hesaplara eklenir. Lokasyon varsa hiçbir şey değişmez.
 * Hesap açmak lokasyona bağlı değildir; bu runner veritabanı boşaltıldıktan sonra da sistemin kullanılabilir kalmasını
 * sağlar. AccountBootstrap'ten sonra çalışır (o, en yüksek öncelikle).
 */
@Component
final class SiteBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteBootstrap.class);
    private static final String DEFAULT_NAME = "Merkez";
    private static final UUID SYSTEM = new UUID(0L, 0L);

    private final JdbcClient jdbc;
    private final Clock clock;

    SiteBootstrap(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        Long existing = jdbc.sql("SELECT count(*) FROM md_site").query(Long.class).single();
        if (existing == 0) {
            UUID id = UUID.randomUUID();
            jdbc.sql("INSERT INTO md_site (id, name, created_at, created_by) VALUES (:id, :name, :at, :actor)")
                    .param("id", id)
                    .param("name", DEFAULT_NAME)
                    .param("at", Timestamp.from(clock.instant()))
                    .param("actor", SYSTEM)
                    .update();
            log.info("Default site created name={}", DEFAULT_NAME);
        }
        int granted = jdbc.sql("""
                        UPDATE app_user
                           SET site_ids = (SELECT array_agg(id) FROM md_site WHERE active)
                         WHERE cardinality(site_ids) = 0 AND EXISTS (SELECT 1 FROM md_site WHERE active)""")
                .update();
        if (granted > 0) {
            log.info("Active sites granted to accounts without sites count={}", granted);
        }
    }
}
