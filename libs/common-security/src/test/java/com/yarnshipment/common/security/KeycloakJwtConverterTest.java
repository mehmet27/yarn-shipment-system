package com.yarnshipment.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class KeycloakJwtConverterTest {

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter(new SecurityProperties(
            Set.of(Role.ADMIN, Role.QUALITY), Set.of("otp"), Set.of("2"), List.of("/actuator/health")));

    @Test
    @DisplayName("Depo kullanıcısının token'ı rol, izin ve lokasyonlarıyla aktöre çevrilir")
    void shouldConvertWarehouseToken() {
        // given
        Jwt jwt = TestTokens.withRoles("warehouse", "offline_access").build();

        // when
        Actor actor = converter.toActor(jwt);

        // then
        assertThat(actor.roles()).containsExactly(Role.WAREHOUSE);
        assertThat(actor.hasPermission(Permission.DISPATCH_RECORD)).isTrue();
        assertThat(actor.siteIds()).containsExactly(TestTokens.SITE_ID);
        assertThat(actor.toString()).doesNotContain("test.user");
    }

    @Test
    @DisplayName("Spring yetkileri izin adlarını ve ROLE_ önekli rolleri içerir")
    void shouldExposePermissionsAndRolesAsAuthorities() {
        // when
        var authentication = converter.convert(TestTokens.withRoles("manager").build());

        // then
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("REPORT_VIEW", "ROLE_MANAGER");
    }

    @Test
    @DisplayName("MFA'sız oturumda admin rolü düşer")
    void shouldDropAdminRoleWithoutMfa() {
        // when
        Actor actor = converter.toActor(TestTokens.withRoles("admin").build());

        // then
        assertThat(actor.roles()).isEmpty();
        assertThat(actor.permissions()).isEmpty();
        assertThat(actor.mfaVerified()).isFalse();
    }

    @Test
    @DisplayName("amr veya acr MFA gösteriyorsa admin rolü geçerlidir")
    void shouldKeepAdminRoleWithMfa() {
        // when
        Actor byAmr = converter.toActor(TestTokens.withMfa("admin").build());
        Actor byAcr = converter.toActor(
                TestTokens.withRoles("quality").claim(TokenClaims.ACR, "2").build());

        // then
        assertThat(byAmr.roles()).containsExactly(Role.ADMIN);
        assertThat(byAcr.roles()).containsExactly(Role.QUALITY);
    }

    @Test
    @DisplayName("UUID olmayan kimlik claim'i reddedilir")
    void shouldRejectNonUuidClaims() {
        // given
        Jwt badSubject = TestTokens.withRoles("warehouse").subject("not-a-uuid").build();
        Jwt badSite = TestTokens.withRoles("warehouse")
                .claim(TokenClaims.SITE_IDS, List.of("x"))
                .build();

        // when / then
        assertThatThrownBy(() -> converter.toActor(badSubject)).isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> converter.toActor(badSite)).isInstanceOf(InvalidBearerTokenException.class);
    }
}
