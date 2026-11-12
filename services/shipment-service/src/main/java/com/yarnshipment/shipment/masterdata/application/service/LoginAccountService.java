package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListUserNamesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Giriş hesaplarını listeler (KR-17: fasoncuya giriş hesabı açılmaz). Yerel veritabanına yazılmadığı için
 * transaction açılmaz. Kullanıcı sayısı küçük olduğundan liste Keycloak'tan
 * bütün olarak alınır, arama ve sayfalama burada yapılır.
 */
@Service
public final class LoginAccountService implements ListLoginAccountsUseCase, ListUserNamesUseCase {

    private static final String QUALITY = "QUALITY";

    private final LoginAccountPort accounts;
    private final StaffWorkAreaPort workAreas;

    public LoginAccountService(LoginAccountPort accounts, StaffWorkAreaPort workAreas) {
        this.accounts = accounts;
        this.workAreas = workAreas;
    }

    @Override
    public MasterDataPage<LoginAccountSummary> listLoginAccounts(MasterDataQuery query) {
        String text = query.text();
        List<LoginAccountSummary> matching = accounts.findAll().stream()
                .filter(account -> text == null || text.isBlank() || account.matches(text))
                .toList();
        long offset = (long) query.page() * query.size();
        Map<UUID, StaffWorkArea> areas = workAreas.findAll();
        List<LoginAccountSummary> items = matching.stream()
                .skip(offset)
                .limit(query.size())
                .map(account -> withWorkArea(account, areas))
                .toList();
        return new MasterDataPage<>(items, query.page(), query.size(), matching.size());
    }

    private static LoginAccountSummary withWorkArea(LoginAccountSummary account, Map<UUID, StaffWorkArea> areas) {
        if (!account.roles().contains(QUALITY)) {
            return account;
        }
        try {
            return account.withWorkArea(areas.get(UUID.fromString(account.id())));
        } catch (IllegalArgumentException notUuid) {
            return account;
        }
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
}
