package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.LoginAccountsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountView;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import com.yarnshipment.shipment.masterdata.application.port.in.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giriş hesabı olan kullanıcıların listesi; yalnızca USER_MANAGE yetkisi olan yönetici. Yanıtta şifre yoktur, yine de
 * kişisel veri içerdiği için önbelleğe alınmaz. Method security proxy'si gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class LoginAccountController implements LoginAccountsApi {

    private final ListLoginAccountsUseCase listLoginAccounts;

    public LoginAccountController(ListLoginAccountsUseCase listLoginAccounts) {
        this.listLoginAccounts = listLoginAccounts;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<LoginAccountPageResponse> listLoginAccounts(
            Integer page, Integer size, @Nullable String search) {
        MasterDataPage<LoginAccountSummary> found =
                listLoginAccounts.listLoginAccounts(new MasterDataQuery(search, page, size));
        LoginAccountPageResponse response = new LoginAccountPageResponse(
                found.items().stream().map(LoginAccountController::view).toList(), MasterDataPages.metadata(found));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    private static LoginAccountView view(LoginAccountSummary account) {
        return new LoginAccountView()
                .id(account.id())
                .username(account.username())
                .fullName(account.fullName())
                .email(account.email())
                .roles(account.roles().stream().map(Role::valueOf).toList())
                .enabled(account.enabled())
                .passwordChangeRequired(account.passwordChangeRequired())
                .createdAt(utc(account.createdAt()));
    }

    private static @Nullable OffsetDateTime utc(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
