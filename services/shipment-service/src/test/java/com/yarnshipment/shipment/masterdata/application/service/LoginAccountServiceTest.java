package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginAccountServiceTest {

    private static final int PAGE_SIZE = 20;
    private static final int ACCOUNT_COUNT = 3;

    private final List<LoginAccountSummary> existing = new ArrayList<>();
    private final LoginAccountPort accounts = new LoginAccountPort() {
        @Override
        public List<LoginAccountSummary> findAll() {
            return List.copyOf(existing);
        }
    };
    private final LoginAccountService service = new LoginAccountService(accounts);

    @Test
    @DisplayName("Hesap listesi giriş adı ve adda büyük-küçük harf duyarsız aranır ve sayfalanır")
    void shouldSearchAndPageLoginAccounts() {
        existing.add(summary("ali.yilmaz", "Ali Yılmaz"));
        existing.add(summary("depo", "Depo Sorumlusu"));
        existing.add(summary("fasoncu", "Örnek Tekstil"));

        MasterDataPage<LoginAccountSummary> byUsername =
                service.listLoginAccounts(new MasterDataQuery("ALI.YIL", 0, PAGE_SIZE));
        MasterDataPage<LoginAccountSummary> secondPage = service.listLoginAccounts(new MasterDataQuery(null, 1, 2));
        MasterDataPage<LoginAccountSummary> byName =
                service.listLoginAccounts(new MasterDataQuery("sorumlu", 0, PAGE_SIZE));

        assertThat(byUsername.items()).extracting(LoginAccountSummary::username).containsExactly("ali.yilmaz");
        assertThat(byUsername.totalElements()).isEqualTo(1);
        assertThat(secondPage.items()).extracting(LoginAccountSummary::username).containsExactly("fasoncu");
        assertThat(secondPage.totalElements()).isEqualTo(ACCOUNT_COUNT);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(byName.items()).extracting(LoginAccountSummary::username).containsExactly("depo");
    }

    private static LoginAccountSummary summary(String username, String fullName) {
        return new LoginAccountSummary(username, username, fullName, List.of("WAREHOUSE"), true, false, null);
    }
}
