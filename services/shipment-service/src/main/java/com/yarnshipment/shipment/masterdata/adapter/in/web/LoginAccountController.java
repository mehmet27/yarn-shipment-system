package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.LoginAccountsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ChangeLoginAccountRoleRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountView;
import com.yarnshipment.shipment.adapter.in.web.api.model.Role;
import com.yarnshipment.shipment.adapter.in.web.api.model.StaffLoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ChangeLoginAccountRoleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateStaffLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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
    private final CreateStaffLoginAccountUseCase createStaff;
    private final ChangeLoginAccountRoleUseCase changeRole;
    private final CurrentActorProvider currentActorProvider;

    public LoginAccountController(
            ListLoginAccountsUseCase listLoginAccounts,
            CreateStaffLoginAccountUseCase createStaff,
            ChangeLoginAccountRoleUseCase changeRole,
            CurrentActorProvider currentActorProvider) {
        this.listLoginAccounts = listLoginAccounts;
        this.createStaff = createStaff;
        this.changeRole = changeRole;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> createStaffLoginAccount(StaffLoginAccountRequest request) {
        createStaff.createStaffLoginAccount(
                currentActorProvider.getCurrentActor(),
                new StaffAccountRequest(
                        request.getUsername(),
                        request.getFullName(),
                        StaffRole.valueOf(request.getRole().name()),
                        request.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @Override
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<Void> changeLoginAccountRole(UUID userId, ChangeLoginAccountRoleRequest request) {
        changeRole.changeLoginAccountRole(
                currentActorProvider.getCurrentActor(),
                userId,
                StaffRole.valueOf(request.getRole().name()));
        return ResponseEntity.noContent().build();
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
                .roles(account.roles().stream().map(Role::valueOf).toList())
                .enabled(account.enabled())
                .passwordChangeRequired(account.passwordChangeRequired())
                .createdAt(utc(account.createdAt()));
    }

    private static @Nullable OffsetDateTime utc(@Nullable Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
