package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.common.security.session.SessionTerminator;
import com.yarnshipment.shipment.masterdata.application.port.out.AccountCredentialPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Hesap yazımının JDBC uygulaması: fabrika içi hesap açma, rol ve ad değişikliği ({@link StaffAccountPort}), geçici
 * şifre, hesabı açma ve kapama ({@link AccountCredentialPort}). Rol, durum ya da şifre değişince kişinin açık
 * oturumları hemen kapanır; kişi yeni yetkiyle yeniden giriş yapar. Şifrenin yalnızca özeti gelir.
 */
@Component
final class StaffAccountAdapter implements StaffAccountPort, AccountCredentialPort {

    private static final String INSERT = """
            INSERT INTO app_user (id, username, full_name, role, password_hash, site_ids, password_change_required,
                                  created_at, updated_at)
            VALUES (:id, :username, :fullName, :role, :hash, CAST(:siteIds AS uuid[]), true, :now, :now)""";

    private final JdbcClient jdbc;
    private final SessionTerminator sessions;
    private final Clock clock;

    StaffAccountAdapter(JdbcClient jdbc, SessionTerminator sessions, Clock clock) {
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public UUID createStaff(StaffAccountSpec spec) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql(INSERT)
                    .param("id", id)
                    .param("username", spec.username().strip())
                    .param("fullName", spec.fullName().strip())
                    .param("role", spec.role().name())
                    .param("hash", spec.passwordHash())
                    .param("siteIds", arrayLiteral(spec))
                    .param("now", now())
                    .update();
        } catch (DuplicateKeyException duplicate) {
            throw new LoginAccountExistsException("username");
        }
        return id;
    }

    @Override
    public void changeStaffRole(UUID userId, StaffRole role) {
        requireUpdated(jdbc.sql("UPDATE app_user SET role = :role, updated_at = :now WHERE id = :id")
                .param("role", role.name())
                .param("now", now())
                .param("id", userId)
                .update());
        sessions.terminateAll(userId);
    }

    @Override
    public void changeStaffName(UUID userId, String fullName) {
        requireUpdated(jdbc.sql("UPDATE app_user SET full_name = :name, updated_at = :now WHERE id = :id")
                .param("name", fullName.strip())
                .param("now", now())
                .param("id", userId)
                .update());
    }

    @Override
    public void setTemporaryPasswordHash(UUID userId, String passwordHash) {
        requireUpdated(jdbc.sql("""
                        UPDATE app_user
                           SET password_hash = :hash, password_change_required = true, failed_attempts = 0,
                               locked_until = NULL, updated_at = :now
                         WHERE id = :id""")
                .param("hash", passwordHash)
                .param("now", now())
                .param("id", userId)
                .update());
        sessions.terminateAll(userId);
    }

    @Override
    public void enable(UUID userId) {
        switchEnabled(userId, true);
    }

    @Override
    public void disable(UUID userId) {
        switchEnabled(userId, false);
        sessions.terminateAll(userId);
    }

    private void switchEnabled(UUID userId, boolean enabled) {
        requireUpdated(jdbc.sql("""
                        UPDATE app_user SET enabled = :enabled, failed_attempts = 0, locked_until = NULL,
                               updated_at = :now
                         WHERE id = :id""")
                .param("enabled", enabled)
                .param("now", now())
                .param("id", userId)
                .update());
    }

    private Timestamp now() {
        return Timestamp.from(clock.instant());
    }

    private static void requireUpdated(int rows) {
        if (rows == 0) {
            throw new LoginAccountNotFoundException();
        }
    }

    private static String arrayLiteral(StaffAccountSpec spec) {
        return spec.siteIds().stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }
}
