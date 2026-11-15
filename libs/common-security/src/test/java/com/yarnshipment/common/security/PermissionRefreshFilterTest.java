package com.yarnshipment.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Yönetici bir rolün iznini değiştirince açık oturum yeniden giriş istemeden yeni izinle çalışır. */
class PermissionRefreshFilterTest {

    private static final Set<Permission> OLD = EnumSet.of(Permission.SHIPMENT_VIEW);
    private static final Set<Permission> NEW = EnumSet.of(Permission.SHIPMENT_VIEW, Permission.REPORT_VIEW);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Rolün izni değişmişse istek yeni izinlerle ve yeni yetki listesiyle çalışır")
    void shouldRefreshChangedPermissions() throws Exception {
        Authentication seen = run(token(Set.of(Role.WAREHOUSE), OLD, false), NEW);

        ActorAuthenticationToken token = (ActorAuthenticationToken) seen;
        assertThat(token.actor().permissions()).isEqualTo(NEW);
        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SHIPMENT_VIEW", "REPORT_VIEW", "ROLE_WAREHOUSE");
        assertThat(token.fullName()).isEqualTo("Test Kullanıcı");
    }

    @Test
    @DisplayName("İzin değişmemişse oturumdaki kimlik olduğu gibi kalır")
    void shouldKeepUnchanged() throws Exception {
        ActorAuthenticationToken original = token(Set.of(Role.WAREHOUSE), OLD, false);

        assertThat(run(original, OLD)).isSameAs(original);
    }

    @Test
    @DisplayName("Şifre değiştirmesi gereken kullanıcıya izin verilmez")
    void shouldNotGrantToPasswordChangeRequired() throws Exception {
        ActorAuthenticationToken original = token(Set.of(), Set.of(), true);

        assertThat(run(original, NEW)).isSameAs(original);
    }

    private static Authentication run(ActorAuthenticationToken token, Set<Permission> fresh) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(token);
        Authentication[] seen = new Authentication[1];
        FilterChain chain = (request, response) -> seen[0] = java.util.Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication());
        new PermissionRefreshFilter(() -> roles -> fresh)
                .doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        return seen[0];
    }

    private static ActorAuthenticationToken token(Set<Role> roles, Set<Permission> permissions, boolean pwChange) {
        Actor actor = new Actor(UUID.randomUUID(), "test.user", roles, permissions, Set.of(), false);
        List<GrantedAuthority> authorities = Stream.<GrantedAuthority>concat(
                        permissions.stream(), roles.stream().map(role -> new SimpleGrantedAuthority(role.authority())))
                .toList();
        return new ActorAuthenticationToken(authorities, actor, "Test Kullanıcı", pwChange);
    }
}
