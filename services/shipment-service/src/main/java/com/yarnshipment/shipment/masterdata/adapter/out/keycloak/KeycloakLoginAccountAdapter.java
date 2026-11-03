package com.yarnshipment.shipment.masterdata.adapter.out.keycloak;

import com.yarnshipment.common.security.Role;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountExistsException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link LoginAccountPort}'un Keycloak Yönetim API'si uygulaması (şoför/fasoncu hesabı açma ve liste). Kullanıcı
 * açıldıktan sonra rol atanamazsa kullanıcı silinir, yarım hesap kalmaz. Liste, uygulama rollerinin üyeleri
 * birleştirilerek çıkarılır; böylece rolsüz servis hesapları listeye girmez ve kullanıcı başına ek çağrı gerekmez.
 */
@Component
final class KeycloakLoginAccountAdapter implements LoginAccountPort {

    private static final int PAGE_SIZE = 100;
    private static final String SITE_IDS = "site_ids";

    private final KeycloakAdminClient admin;

    KeycloakLoginAccountAdapter(KeycloakAdminClient admin) {
        this.admin = admin;
    }

    @Override
    public void create(LoginAccountSpec spec) {
        admin.guarded(() -> {
            String token = admin.accessToken();
            requireNoAccountFor(token, spec);
            Map<String, Object> role = admin.realmRole(token, spec.role().realmRole());
            String userId = admin.createUser(token, userRepresentation(spec));
            admin.rollbackOnFailure(token, userId, () -> admin.assignRole(token, userId, role));
            return null;
        });
    }

    @Override
    public List<LoginAccountSummary> findAll() {
        return admin.guarded(() -> {
            String token = admin.accessToken();
            Map<Role, List<Map<String, Object>>> membersByRole = new EnumMap<>(Role.class);
            for (Role role : KeycloakAccountSummaries.listedRoles()) {
                membersByRole.put(role, membersOf(token, role));
            }
            return KeycloakAccountSummaries.merge(membersByRole);
        });
    }

    /** Rolün doğrudan üyeleri, sayfa sayfa; realm'de tanımlı olmayan rol boş döner. */
    private List<Map<String, Object>> membersOf(String token, Role role) {
        String realmRole = role.name().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> members = new ArrayList<>();
        List<Map<String, Object>> page;
        do {
            page = memberPage(token, realmRole, members.size());
            members.addAll(page);
        } while (page.size() == PAGE_SIZE);
        return members;
    }

    private List<Map<String, Object>> memberPage(String token, String realmRole, int first) {
        try {
            List<Map<String, Object>> page = admin.http()
                    .get()
                    .uri(uri -> uri.path("/admin/realms/{realm}/roles/{role}/users")
                            .queryParam("first", first)
                            .queryParam("max", PAGE_SIZE)
                            .queryParam("briefRepresentation", false)
                            .build(admin.realm(), realmRole))
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(KeycloakAdminClient.OBJECTS);
            return page == null ? List.of() : page;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return List.of();
            }
            throw exception;
        }
    }

    private void requireNoAccountFor(String token, LoginAccountSpec spec) {
        List<Map<String, Object>> existing = admin.http()
                .get()
                .uri(uri -> uri.path("/admin/realms/{realm}/users")
                        .queryParam("q", spec.role().ownerAttribute() + ":" + spec.ownerId())
                        .build(admin.realm()))
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(KeycloakAdminClient.OBJECTS);
        if (existing != null && !existing.isEmpty()) {
            throw new LoginAccountExistsException("record");
        }
    }

    private static Map<String, Object> userRepresentation(LoginAccountSpec spec) {
        String[] names = KeycloakAdminClient.splitName(spec.displayName());
        LoginRole role = spec.role();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", spec.username());
        user.put("enabled", true);
        user.put("firstName", names[0]);
        user.put("lastName", names[1]);
        user.put("email", spec.email());
        user.put(
                "attributes",
                Map.of(
                        role.ownerAttribute(),
                        List.of(spec.ownerId().toString()),
                        SITE_IDS,
                        spec.siteIds().stream().map(Object::toString).toList()));
        user.put("requiredActions", List.of("UPDATE_PASSWORD"));
        user.put(
                "credentials",
                List.of(Map.of("type", "password", "value", spec.temporaryPassword(), "temporary", true)));
        return user;
    }
}
