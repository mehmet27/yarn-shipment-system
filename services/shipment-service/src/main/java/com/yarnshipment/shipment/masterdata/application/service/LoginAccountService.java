package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListLoginAccountsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.account.ListUserNamesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
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

    private final LoginAccountPort accounts;

    public LoginAccountService(LoginAccountPort accounts) {
        this.accounts = accounts;
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

}
