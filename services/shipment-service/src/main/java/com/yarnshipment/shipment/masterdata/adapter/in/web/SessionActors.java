package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.ActorAuthenticationToken;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.RolePermissionMatrix;
import com.yarnshipment.shipment.masterdata.domain.model.SessionUser;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Giriş yapan kullanıcıdan oturum kimliğini üretir. Şifresini değiştirmesi gereken kişiye rol ve izin verilmez; bu
 * durumda yalnızca kendi şifresini değiştirebilir ve oturumunu kapatabilir.
 */
final class SessionActors {

    private SessionActors() {}

    static ActorAuthenticationToken tokenOf(SessionUser user) {
        Set<Role> roles = user.passwordChangeRequired() ? Set.of() : EnumSet.of(Role.valueOf(user.role().name()));
        Set<Permission> permissions = RolePermissionMatrix.permissionsOf(roles);
        Actor actor = new Actor(user.id(), user.username(), roles, permissions, user.siteIds(), false);
        List<GrantedAuthority> authorities = new ArrayList<>(permissions);
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role.authority())));
        return new ActorAuthenticationToken(authorities, actor, user.fullName(), user.passwordChangeRequired());
    }
}
