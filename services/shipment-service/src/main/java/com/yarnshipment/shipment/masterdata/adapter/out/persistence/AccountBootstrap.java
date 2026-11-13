package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.masterdata.application.port.out.PasswordHasherPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * İlk kurulumda giriş yapılabilen bir hesap sağlar. {@code yarnshipment.auth.bootstrap.admin-password} doluysa
 * {@code admin} kullanıcısı yoksa açılır (ilk girişte şifre değiştirilir); eski sistemden alınmış ve şifresi
 * belirlenmemiş ("!") {@code admin} varsa şifresi bununla belirlenir. Hesap zaten kullanılabiliyorsa hiçbir şey
 * değişmez. Yerel geliştirmede {@code sample-users} açıksa örnek sevkiyat verisiyle eşleşen dört kullanıcı da açılır
 * (şifre değiştirme istenmez). Şifreler loglanmaz.
 */
@Component
final class AccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountBootstrap.class);
    private static final String UNUSABLE = "!";
    private static final String SITE_1 = "0192f0a0-0000-7000-8000-000000000001";
    private static final String SITE_2 = "0192f0a0-0000-7000-8000-000000000002";

    /** Örnek sevkiyat verisindeki kimliklerle eşleşir (R__local_sample_shipments.sql). */
    private static final List<SampleUser> SAMPLES = List.of(
            new SampleUser(id(101), "admin", "Ayşe Yönetici", "ADMIN", SITE_1 + "," + SITE_2),
            new SampleUser(id(102), "depo", "Davut Depocu", "WAREHOUSE", SITE_1),
            new SampleUser(id(103), "kalite", "Kerem Kalite", "QUALITY", SITE_1 + "," + SITE_2),
            new SampleUser(id(104), "mudur", "Melek Müdür", "MANAGER", SITE_1 + "," + SITE_2));

    private final JdbcClient jdbc;
    private final PasswordHasherPort hasher;
    private final String adminPassword;
    private final boolean sampleUsers;
    private final String samplePassword;

    AccountBootstrap(
            JdbcClient jdbc,
            PasswordHasherPort hasher,
            @Value("${yarnshipment.auth.bootstrap.admin-password:}") String adminPassword,
            @Value("${yarnshipment.auth.bootstrap.sample-users:false}") boolean sampleUsers,
            @Value("${yarnshipment.auth.bootstrap.sample-password:}") String samplePassword) {
        this.jdbc = jdbc;
        this.hasher = hasher;
        this.adminPassword = adminPassword;
        this.sampleUsers = sampleUsers;
        this.samplePassword = samplePassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (sampleUsers && !samplePassword.isBlank()) {
            SAMPLES.forEach(this::insertSample);
            log.info("Local sample users ensured count={}", SAMPLES.size());
        } else if (!adminPassword.isBlank()) {
            ensureAdmin();
        }
    }

    private void insertSample(SampleUser user) {
        jdbc.sql("""
                        INSERT INTO app_user (id, username, full_name, role, password_hash, site_ids,
                                              password_change_required)
                        VALUES (:id, :username, :fullName, :role, :hash, CAST(:siteIds AS uuid[]), false)
                        ON CONFLICT DO NOTHING""")
                .param("id", UUID.fromString(user.id()))
                .param("username", user.username())
                .param("fullName", user.fullName())
                .param("role", user.role())
                .param("hash", hasher.hash(samplePassword))
                .param("siteIds", "{" + user.siteIds() + "}")
                .update();
    }

    private void ensureAdmin() {
        String existing = jdbc.sql("SELECT password_hash FROM app_user WHERE lower(username) = 'admin'")
                .query(String.class)
                .optional()
                .orElse(null);
        if (existing == null) {
            jdbc.sql("""
                            INSERT INTO app_user (id, username, full_name, role, password_hash)
                            VALUES (:id, 'admin', 'Sistem Yöneticisi', 'ADMIN', :hash)""")
                    .param("id", UUID.randomUUID())
                    .param("hash", hasher.hash(adminPassword))
                    .update();
            log.info("Initial admin account created");
        } else if (UNUSABLE.equals(existing)) {
            jdbc.sql("""
                            UPDATE app_user SET password_hash = :hash, password_change_required = true, enabled = true
                             WHERE lower(username) = 'admin'""")
                    .param("hash", hasher.hash(adminPassword))
                    .update();
            log.info("Initial admin password set");
        }
    }

    private static String id(int number) {
        return "0192f0a0-0000-7000-8000-000000000" + number;
    }

    private record SampleUser(String id, String username, String fullName, String role, String siteIds) {}
}
