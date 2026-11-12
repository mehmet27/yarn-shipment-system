package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginAccountServiceTest {

    private static final int PAGE_SIZE = 20;
    private static final int ACCOUNT_COUNT = 3;

    private static final UUID QUALITY_USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");

    private final List<LoginAccountSummary> existing = new ArrayList<>();
    private final LoginAccountPort accounts = new LoginAccountPort() {
        @Override
        public List<LoginAccountSummary> findAll() {
            return List.copyOf(existing);
        }
    };
    private final StaffWorkAreaPort workAreas = new StaffWorkAreaPort() {
        @Override
        public void save(UUID userId, StaffWorkArea workArea) {}

        @Override
        public void delete(UUID userId) {}

        @Override
        public Map<UUID, StaffWorkArea> findAll() {
            return Map.of(QUALITY_USER, StaffWorkArea.CENTRAL);
        }
    };
    private final LoginAccountService service = new LoginAccountService(accounts, workAreas);

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

    @Test
    @DisplayName("Kalite rolündeki hesabın listede çalışma alanı gelir; başka rolde gelmez")
    void shouldAttachWorkAreaToQualityAccountsOnly() {
        existing.add(new LoginAccountSummary(
                QUALITY_USER.toString(), "kalite", "Kalite", List.of("QUALITY"), true, false, null));
        existing.add(summary("depo", "Depo"));

        MasterDataPage<LoginAccountSummary> page = service.listLoginAccounts(new MasterDataQuery(null, 0, PAGE_SIZE));

        assertThat(page.items()).extracting(LoginAccountSummary::workArea).containsExactly(StaffWorkArea.CENTRAL, null);
    }

    private static LoginAccountSummary summary(String username, String fullName) {
        return new LoginAccountSummary(username, username, fullName, List.of("WAREHOUSE"), true, false, null);
    }
}
