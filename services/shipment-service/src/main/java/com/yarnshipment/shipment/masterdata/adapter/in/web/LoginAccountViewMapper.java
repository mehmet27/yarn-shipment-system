package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountView;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import com.yarnshipment.shipment.adapter.in.web.api.model.WorkArea;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;

/** Giriş hesabı özetini API görünümüne çevirir; yanıtta şifre yoktur. */
final class LoginAccountViewMapper {

    private LoginAccountViewMapper() {}

    static LoginAccountView view(LoginAccountSummary account) {
        return new LoginAccountView()
                .id(account.id())
                .username(account.username())
                .fullName(account.fullName())
                .roles(account.roles().stream().map(Role::valueOf).toList())
                .enabled(account.enabled())
                .passwordChangeRequired(account.passwordChangeRequired())
                .createdAt(utc(account.createdAt()))
                .workArea(
                        account.workArea() == null
                                ? null
                                : WorkArea.valueOf(account.workArea().name()));
    }

    private static @Nullable OffsetDateTime utc(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
