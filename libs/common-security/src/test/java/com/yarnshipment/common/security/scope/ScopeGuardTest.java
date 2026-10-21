package com.yarnshipment.common.security.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.KeycloakJwtConverter;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.common.security.SecurityProperties;
import com.yarnshipment.common.security.TestTokens;
import com.yarnshipment.common.security.audit.AuditDecision;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScopeGuardTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");

    private final List<SecurityAuditEvent> published = new ArrayList<>();
    private final ScopeGuard scopeGuard = new ScopeGuard(published::add, Clock.fixed(NOW, ZoneOffset.UTC));
    private final KeycloakJwtConverter converter =
            new KeycloakJwtConverter(new SecurityProperties(Set.of(), Set.of("otp"), Set.of("2"), List.of()));

    @Test
    @DisplayName("Fasoncu kendi sevkiyatına erişir, başka fasoncununkine erişemez")
    void shouldLimitSubcontractorToOwnRecords() {
        // given
        Actor actor = converter.toActor(TestTokens.subcontractor());

        // when / then
        assertThat(scopeGuard.isInScope(actor, shipment(TestTokens.SUBCONTRACTOR_ID, null)))
                .isTrue();
        assertThat(scopeGuard.isInScope(actor, shipment(UUID.randomUUID(), null)))
                .isFalse();
    }

    @Test
    @DisplayName("Şoför yalnızca kendisine atanmış sevkiyata erişir")
    void shouldLimitDriverToAssignedRecords() {
        // given
        Actor actor = converter.toActor(TestTokens.driver());

        // when / then
        assertThat(scopeGuard.isInScope(actor, shipment(null, TestTokens.DRIVER_ID)))
                .isTrue();
        assertThat(scopeGuard.isInScope(actor, shipment(null, UUID.randomUUID())))
                .isFalse();
    }

    @Test
    @DisplayName("İç kullanıcı yalnızca yetkili olduğu lokasyondaki kayda erişir")
    void shouldLimitInternalUserToOwnSites() {
        // given
        Actor actor = converter.toActor(TestTokens.withRoles("warehouse").build());
        ScopedResource otherSite =
                new ScopedResource("Shipment", UUID.randomUUID(), TestTokens.OTHER_SITE_ID, null, null);

        // when / then
        assertThat(scopeGuard.isInScope(actor, shipment(UUID.randomUUID(), null)))
                .isTrue();
        assertThat(scopeGuard.isInScope(actor, otherSite)).isFalse();
    }

    @Test
    @DisplayName("Sistem hesabının kapsamı kısıtsızdır")
    void shouldGiveSystemUnrestrictedScope() {
        // given
        Actor system = new Actor(
                UUID.randomUUID(),
                "svc",
                Set.of(Role.SYSTEM),
                RolePermissionMatrix.permissionsOf(Set.of(Role.SYSTEM)),
                null,
                null,
                Set.of(),
                false);

        // when / then
        assertThat(scopeGuard.scopeOf(system)).isEqualTo(AccessScope.UNRESTRICTED);
        assertThat(scopeGuard.isInScope(system, shipment(UUID.randomUUID(), null)))
                .isTrue();
    }

    @Test
    @DisplayName("Kapsam dışı erişim 403 istisnası fırlatır ve audit'e yazılır")
    void shouldAuditAndRejectOutOfScopeAccess() {
        // given
        Actor actor = converter.toActor(TestTokens.subcontractor());
        ScopedResource resource = shipment(UUID.randomUUID(), null);

        // when / then
        assertThatThrownBy(() -> scopeGuard.validateInScope(actor, resource, "SHIPMENT_VIEW"))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(published).singleElement().satisfies(event -> {
            assertThat(event.decision()).isEqualTo(AuditDecision.DENIED);
            assertThat(event.reason()).isEqualTo("OUT_OF_SCOPE");
            assertThat(event.resource()).isEqualTo(resource.reference());
            assertThat(event.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Kapsam içindeki erişim audit'e ret yazmaz")
    void shouldNotAuditInScopeAccess() {
        // given
        Actor actor = converter.toActor(TestTokens.subcontractor());

        // when
        scopeGuard.validateInScope(actor, shipment(TestTokens.SUBCONTRACTOR_ID, null), "SHIPMENT_VIEW");

        // then
        assertThat(published).isEmpty();
    }

    private static ScopedResource shipment(@Nullable UUID subcontractorId, @Nullable UUID driverId) {
        return new ScopedResource("Shipment", UUID.randomUUID(), TestTokens.SITE_ID, subcontractorId, driverId);
    }
}
