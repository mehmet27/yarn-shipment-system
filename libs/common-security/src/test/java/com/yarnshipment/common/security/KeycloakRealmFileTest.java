package com.yarnshipment.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

/**
 * Yerel Keycloak realm dosyası ile servislerin beklediği token biçimi aynı kalır: Keycloak çalıştırmadan, dosyadaki
 * mapper ve kullanıcılardan token'ı kurup {@link KeycloakJwtConverter} ile çözer.
 */
class KeycloakRealmFileTest {

    private static final Path REALM_FILE = Path.of("..", "..", "deploy", "keycloak", "yarnshipment-realm-local.json");
    private static final String CLAIM_NAME = "claim.name";
    private static final String GATEWAY = "yarnshipment-gateway";
    private static final Instant ISSUED_AT = Instant.parse("2026-10-21T08:00:00Z");

    private final RealmFile realm = JsonMapper.shared().readValue(REALM_FILE.toFile(), RealmFile.class);
    private final KeycloakJwtConverter converter =
            new KeycloakJwtConverter(new SecurityProperties(Set.of(), Set.of("otp"), Set.of("2"), List.of()));

    @Test
    @DisplayName("Realm rolleri Role enum'uyla birebir aynıdır")
    void shouldDefineExactlyTheServiceRoles() {
        // given
        Set<String> expected = Arrays.stream(Role.values())
                .map(role -> role.name().toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());

        // when
        Set<String> defined = realm.roles().realm().stream().map(Named::name).collect(Collectors.toSet());

        // then
        assertThat(defined).isEqualTo(expected);
    }

    @Test
    @DisplayName("Gateway istemcisi servislerin okuduğu claim'leri üretir")
    void shouldMapTheClaimsServicesRead() {
        // when
        Set<String> claims = mappersOf(GATEWAY).stream()
                .map(mapper -> mapper.config().get(CLAIM_NAME))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // then
        assertThat(claims)
                .containsExactlyInAnyOrder(
                        TokenClaims.ROLES, TokenClaims.SUBCONTRACTOR_ID, TokenClaims.DRIVER_ID, TokenClaims.SITE_IDS);
        assertThat(mappersOf(GATEWAY).stream().map(Mapper::name)).contains("audience-yarnshipment-api");
    }

    @Test
    @DisplayName("Doğrulama istemcisi gateway ile aynı mapper'ları taşır")
    void shouldKeepDevToolsClientAlignedWithGateway() {
        assertThat(mappersOf("yarnshipment-dev-tools")).isEqualTo(mappersOf(GATEWAY));
    }

    @Test
    @DisplayName("Her test kullanıcısının token'ı geçerli bir aktöre çevrilir")
    void shouldConvertEveryTestUserToAnActor() {
        // when
        Map<String, Actor> actors = realm.users().stream()
                .collect(Collectors.toMap(User::username, user -> converter.toActor(tokenOf(user))));

        // then
        assertThat(actors).containsOnlyKeys("admin", "depo", "kalite", "mudur", "sofor", "fasoncu");
        assertThat(actors).extractingByKey("sofor").extracting(Actor::driverId).isEqualTo(TestTokens.DRIVER_ID);
        assertThat(actors)
                .extractingByKey("fasoncu")
                .extracting(Actor::subcontractorId)
                .isEqualTo(TestTokens.SUBCONTRACTOR_ID);
        assertThat(actors).extractingByKey("admin").extracting(Actor::roles).isEqualTo(Set.of(Role.ADMIN));
        assertThat(actors.values())
                .allSatisfy(actor -> assertThat(actor.siteIds()).isNotEmpty());
    }

    private List<Mapper> mappersOf(String clientId) {
        return realm.clients().stream()
                .filter(client -> client.clientId().equals(clientId))
                .findFirst()
                .map(Client::protocolMappers)
                .orElseThrow();
    }

    /** Keycloak'un mapper'larla üreteceği claim'lerin aynısını kullanıcının özniteliklerinden kurar. */
    private static Jwt tokenOf(User user) {
        Jwt.Builder builder = Jwt.withTokenValue("token-" + user.username())
                .header("alg", "RS256")
                .issuedAt(ISSUED_AT)
                .expiresAt(ISSUED_AT.plusSeconds(1))
                .subject(user.id())
                .claim(TokenClaims.PREFERRED_USERNAME, user.username())
                .claim(TokenClaims.ROLES, user.realmRoles());
        user.attributes().forEach((name, values) -> builder.claim(name, claimValue(name, values)));
        return builder.build();
    }

    private static Object claimValue(String name, List<String> values) {
        return TokenClaims.SITE_IDS.equals(name) ? values : values.getFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RealmFile(RealmRoles roles, List<Client> clients, List<User> users) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RealmRoles(List<Named> realm) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Named(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Client(String clientId, @Nullable List<Mapper> protocolMappers) {

        @Override
        public @Nullable List<Mapper> protocolMappers() {
            return protocolMappers == null ? List.of() : protocolMappers;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Mapper(String name, Map<String, String> config) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record User(String id, String username, Map<String, List<String>> attributes, List<String> realmRoles) {}
}
