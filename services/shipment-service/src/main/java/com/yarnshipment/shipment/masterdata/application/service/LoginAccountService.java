package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateDriverLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.CreateSubcontractorLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.IssueActivationCodeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListUserNamesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.LoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.PhoneNumber;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Şoför ve fasoncu için giriş hesabı açar ve giriş hesaplarını listeler. Kayıt ve lokasyonlar etkin olmalıdır; hesap
 * Keycloak'ta şifresiz tek çağrı dizisiyle açılır ve yarım kalırsa geri alınır, ardından şifre belirleme kodu SMS'le
 * gider. Yerel veritabanına yazılmadığı için transaction
 * açılmaz. Kullanıcı sayısı küçük olduğundan liste Keycloak'tan bütün olarak alınır, arama ve sayfalama burada yapılır.
 */
@Service
public final class LoginAccountService
        implements CreateDriverLoginAccountUseCase,
                CreateSubcontractorLoginAccountUseCase,
                ListLoginAccountsUseCase,
                ListUserNamesUseCase {

    private final LoginAccountTargetPort targets;
    private final LoginAccountPort accounts;
    private final IssueActivationCodeUseCase activation;

    public LoginAccountService(
            LoginAccountTargetPort targets, LoginAccountPort accounts, IssueActivationCodeUseCase activation) {
        this.targets = targets;
        this.accounts = accounts;
        this.activation = activation;
    }

    @Override
    public void createDriverLoginAccount(Actor actor, UUID driverId, LoginAccountRequest request) {
        String name = targets.findActiveDriverName(driverId)
                .orElseThrow(() -> new MasterDataNotFoundException("Driver", driverId));
        open(LoginRole.DRIVER, driverId, name, request);
    }

    @Override
    public void createSubcontractorLoginAccount(Actor actor, UUID subcontractorId, LoginAccountRequest request) {
        Optional<String> title = targets.findActiveSubcontractorTitle(subcontractorId);
        open(
                LoginRole.SUBCONTRACTOR,
                subcontractorId,
                title.orElseThrow(() -> new MasterDataNotFoundException("Subcontractor", subcontractorId)),
                request);
    }

    @Override
    public MasterDataPage<LoginAccountSummary> listLoginAccounts(MasterDataQuery query) {
        String text = query.text();
        List<LoginAccountSummary> matching = accounts.findAll().stream()
                .filter(account -> text == null || text.isBlank() || account.matches(text))
                .toList();
        long offset = (long) query.page() * query.size();
        List<LoginAccountSummary> items =
                matching.stream().skip(offset).limit(query.size()).toList();
        return new MasterDataPage<>(items, query.page(), query.size(), matching.size());
    }

    @Override
    public Map<UUID, String> listUserNames(Collection<UUID> userIds) {
        Map<String, UUID> wanted = new HashMap<>();
        userIds.forEach(id -> wanted.put(id.toString(), id));
        Map<UUID, String> names = new HashMap<>();
        for (LoginAccountSummary account : accounts.findAll()) {
            UUID userId = wanted.get(account.id().toLowerCase(Locale.ROOT));
            if (userId != null) {
                String fullName = account.fullName();
                names.put(userId, fullName == null || fullName.isBlank() ? account.username() : fullName);
            }
        }
        return names;
    }

    private void requireActiveSites(List<UUID> siteIds) {
        if (siteIds.isEmpty() || !targets.findActiveSiteIds(siteIds).containsAll(new HashSet<>(siteIds))) {
            throw new InvalidMasterDataException("siteIds", "empty or unknown or inactive");
        }
    }

    private void open(LoginRole role, UUID ownerId, String displayName, LoginAccountRequest request) {
        PhoneNumber phone = PhoneNumber.parse(request.phone());
        requireActiveSites(request.siteIds());
        UUID userId = accounts.create(new LoginAccountSpec(
                request.username(), request.email(), phone, displayName, role, ownerId, request.siteIds()));
        activation.issueActivationCode(userId, request.username(), phone);
    }
}
