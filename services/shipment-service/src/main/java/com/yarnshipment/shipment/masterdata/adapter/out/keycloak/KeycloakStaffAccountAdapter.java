package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountRoleLockedException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link StaffAccountPort}'un Keycloak uygulaması. Fabrika içi kullanıcı şifresiz açılır; şifreyi kişi e-postadaki
 * bağlantıdan belirler. E-posta gidemezse (ör. SMTP yok) açılan kullanıcı silinir. Şifre hiçbir yerde üretilmez.
 */
@Component
final class KeycloakStaffAccountAdapter implements StaffAccountPort {

    private static final int INVITE_LIFESPAN_SECONDS = 3 * 24 * 60 * 60;
    private static final String SITE_IDS = "site_ids";
    private static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";

    private final KeycloakAdminClient admin;

    KeycloakStaffAccountAdapter(KeycloakAdminClient admin) {
        this.admin = admin;
    }

    @Override
    public void createStaff(StaffAccountSpec spec) {
        admin.guardedRun(() -> open(spec));
    }

    @Override
    public void changeStaffRole(UUID userId, StaffRole role) {
        admin.guardedRun(() -> replaceRole(userId.toString(), role));
    }

    private void open(StaffAccountSpec spec) {
        String token = admin.accessToken();
        Map<String, Object> role = admin.realmRole(token, spec.role().realmRole());
        String userId = admin.createUser(token, representation(spec));
        admin.rollbackOnFailure(token, userId, () -> {
            admin.assignRole(token, userId, role);
            sendPasswordInvite(token, userId);
        });
    }

    private void replaceRole(String userId, StaffRole role) {
        String token = admin.accessToken();
        List<Map<String, Object>> current = currentRealmRoles(token, userId);
        Set<String> names = new HashSet<>();
        current.forEach(mapping -> names.add(String.valueOf(mapping.get("name"))));
        if (names.contains(LoginRole.DRIVER.realmRole()) || names.contains(LoginRole.SUBCONTRACTOR.realmRole())) {
            throw new LoginAccountRoleLockedException();
        }
        if (!names.contains(role.realmRole())) {
            admin.assignRole(token, userId, admin.realmRole(token, role.realmRole()));
        }
        removeOtherStaffRoles(token, userId, current, role);
        endSessions(token, userId);
    }

    private void removeOtherStaffRoles(String token, String userId, List<Map<String, Object>> current, StaffRole keep) {
        List<Map<String, Object>> obsolete = current.stream()
                .filter(mapping -> isStaffRole(String.valueOf(mapping.get("name")))
                        && !keep.realmRole().equals(mapping.get("name")))
                .map(mapping -> Map.of("id", mapping.get("id"), "name", mapping.get("name")))
                .toList();
        if (obsolete.isEmpty()) {
            return;
        }
        admin.http()
                .method(HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", admin.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(obsolete)
                .retrieve()
                .toBodilessEntity();
    }

    private static boolean isStaffRole(String name) {
        return Stream.of(StaffRole.values()).anyMatch(role -> role.realmRole().equals(name));
    }

    private List<Map<String, Object>> currentRealmRoles(String token, String userId) {
        try {
            List<Map<String, Object>> roles = admin.http()
                    .get()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", admin.realm(), userId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(KeycloakAdminClient.OBJECTS);
            return roles == null ? List.of() : roles;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new LoginAccountNotFoundException();
            }
            throw exception;
        }
    }

    /** Eski rolle alınmış token'lar kısa süre geçerli kalır; açık oturumlar kapatılarak yeni rolle giriş istenir. */
    private void endSessions(String token, String userId) {
        admin.http()
                .post()
                .uri("/admin/realms/{realm}/users/{id}/logout", admin.realm(), userId)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .toBodilessEntity();
    }

    private void sendPasswordInvite(String token, String userId) {
        admin.http()
                .put()
                .uri(uri -> uri.path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                        .queryParam("lifespan", INVITE_LIFESPAN_SECONDS)
                        .build(admin.realm(), userId))
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(UPDATE_PASSWORD))
                .retrieve()
                .toBodilessEntity();
    }

    private static Map<String, Object> representation(StaffAccountSpec spec) {
        String[] names = KeycloakAdminClient.splitName(spec.fullName());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", spec.username());
        user.put("enabled", true);
        user.put("firstName", names[0]);
        user.put("lastName", names[1]);
        user.put("email", spec.email());
        user.put(
                "attributes",
                Map.of(SITE_IDS, spec.siteIds().stream().map(Object::toString).toList()));
        user.put("requiredActions", List.of(UPDATE_PASSWORD));
        return user;
    }
}
