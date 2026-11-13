package com.yarnshipment.shipment.masterdata.adapter.out.persistence;

import com.yarnshipment.shipment.application.port.out.ActorNamePort;
import com.yarnshipment.shipment.masterdata.application.port.out.AuthUserPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * {@code app_user} tablosunun JDBC uygulaması: giriş denetimi için okuma ve sayaç yazımı ({@link AuthUserPort}),
 * Kullanıcılar ekranı listesi ({@link LoginAccountPort}) ve kullanıcı kimliklerini adlara çevirme
 * ({@link ActorNamePort}). Şifre özeti listeye ve adlara hiç karışmaz.
 */
@Component
final class AppUserAdapter implements AuthUserPort, LoginAccountPort, ActorNamePort {

    private static final String AUTH_COLUMNS = """
            id, username, full_name, role, password_hash, enabled, password_change_required, locked_until,
            array_to_string(site_ids, ',') AS site_ids""";

    private static final String FAILED_ATTEMPT = """
            UPDATE app_user
               SET failed_attempts = CASE WHEN failed_attempts + 1 >= :maxAttempts THEN 0 ELSE failed_attempts + 1 END,
                   locked_until = CASE WHEN failed_attempts + 1 >= :maxAttempts THEN :lockedUntil ELSE locked_until END,
                   updated_at = :now
             WHERE id = :id""";

    private static final String SUCCESSFUL_LOGIN = """
            UPDATE app_user
               SET failed_attempts = 0, locked_until = NULL, last_login_at = :now, updated_at = :now
             WHERE id = :id""";

    private static final String REPLACE_PASSWORD = """
            UPDATE app_user
               SET password_hash = :hash, password_change_required = false, password_changed_at = :now,
                   failed_attempts = 0, locked_until = NULL, updated_at = :now
             WHERE id = :id""";

    private static final String LIST_ALL = """
            SELECT id, username, full_name, role, enabled, password_change_required, created_at
              FROM app_user ORDER BY lower(username)""";

    private final JdbcClient jdbc;

    AppUserAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return jdbc.sql("SELECT " + AUTH_COLUMNS + " FROM app_user WHERE lower(username) = lower(:username)")
                .param("username", username)
                .query(AppUserAdapter::authUser)
                .optional();
    }

    @Override
    public Optional<AuthUser> findById(UUID userId) {
        return jdbc.sql("SELECT " + AUTH_COLUMNS + " FROM app_user WHERE id = :id")
                .param("id", userId)
                .query(AppUserAdapter::authUser)
                .optional();
    }

    @Override
    public void recordFailedAttempt(UUID userId, int maxAttempts, Duration lockFor, Instant now) {
        jdbc.sql(FAILED_ATTEMPT)
                .param("id", userId)
                .param("maxAttempts", maxAttempts)
                .param("lockedUntil", Timestamp.from(now.plus(lockFor)))
                .param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public void recordSuccessfulLogin(UUID userId, Instant now) {
        jdbc.sql(SUCCESSFUL_LOGIN)
                .param("id", userId)
                .param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public void replacePasswordHash(UUID userId, String passwordHash, Instant now) {
        jdbc.sql(REPLACE_PASSWORD)
                .param("id", userId)
                .param("hash", passwordHash)
                .param("now", Timestamp.from(now))
                .update();
    }

    @Override
    public List<LoginAccountSummary> findAll() {
        return jdbc.sql(LIST_ALL).query(AppUserAdapter::summary).list();
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        jdbc.sql("SELECT id, username, full_name FROM app_user WHERE id IN (:ids)")
                .param("ids", userIds)
                .query((rs, row) -> {
                    names.put(rs.getObject("id", UUID.class), rs.getString("full_name"));
                    return row;
                })
                .list();
        return names;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter") // RowMapper imzası
    private static AuthUser authUser(ResultSet rs, int row) throws SQLException {
        return new AuthUser(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("full_name"),
                StaffRole.valueOf(rs.getString("role")),
                rs.getString("password_hash"),
                rs.getBoolean("enabled"),
                rs.getBoolean("password_change_required"),
                instant(rs.getTimestamp("locked_until")),
                siteIds(rs.getString("site_ids")));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter") // RowMapper imzası
    private static LoginAccountSummary summary(ResultSet rs, int row) throws SQLException {
        return new LoginAccountSummary(
                rs.getObject("id", UUID.class).toString(),
                rs.getString("username"),
                rs.getString("full_name"),
                List.of(rs.getString("role")),
                rs.getBoolean("enabled"),
                rs.getBoolean("password_change_required"),
                instant(rs.getTimestamp("created_at")));
    }

    private static @Nullable Instant instant(@Nullable Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Set<UUID> siteIds(@Nullable String joined) {
        if (joined == null || joined.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(joined.split(",")).map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
    }
}
