package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.account.LoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSummary;
import com.yarnshipment.shipment.masterdata.domain.model.LoginRole;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoginAccountServiceTest {

    private static final int PAGE_SIZE = 20;
    private static final int ACCOUNT_COUNT = 3;
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");

    private static final UUID NEW_USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");

    private final List<LoginAccountSpec> opened = new ArrayList<>();
    private final List<String> codesSent = new ArrayList<>();
    private final Actor admin = TestTokens.actor("admin");
    private final LoginAccountTargetPort targets = new LoginAccountTargetPort() {
        @Override
        public Optional<String> findActiveSubcontractorTitle(UUID id) {
            return SUBCONTRACTOR.equals(id) ? Optional.of("Örnek Tekstil") : Optional.empty();
        }

        @Override
        public Set<UUID> findActiveSiteIds(Collection<UUID> ids) {
            return ids.contains(SITE) ? Set.of(SITE) : Set.of();
        }
    };
    private final List<LoginAccountSummary> existing = new ArrayList<>();
    private final LoginAccountPort accounts = new LoginAccountPort() {
        @Override
        public UUID create(LoginAccountSpec spec) {
            opened.add(spec);
            return NEW_USER;
        }

        @Override
        public List<LoginAccountSummary> findAll() {
            return List.copyOf(existing);
        }
    };
    private final LoginAccountService service = new LoginAccountService(
            targets,
            accounts,
            (userId, username, phone) -> codesSent.add(userId + ":" + username + ":" + phone.e164()));

    @Test
    @DisplayName("Fasoncu hesabı fasoncu rolü ve kayıt kimliğiyle, şifresiz açılır; kod SMS'le gider")
    void shouldOpenSubcontractorAccount() {
        service.createSubcontractorLoginAccount(
                admin, SUBCONTRACTOR, new LoginAccountRequest("ornek", "0532 123 45 67", "a@b.com", List.of(SITE)));

        assertThat(opened).singleElement().satisfies(spec -> {
            assertThat(spec.role()).isEqualTo(LoginRole.SUBCONTRACTOR);
            assertThat(spec.ownerId()).isEqualTo(SUBCONTRACTOR);
            assertThat(spec.displayName()).isEqualTo("Örnek Tekstil");
            assertThat(spec.email()).isEqualTo("a@b.com");
            assertThat(spec.phone().e164()).isEqualTo("+905321234567");
        });
        assertThat(codesSent).containsExactly(NEW_USER + ":ornek:+905321234567");
    }

    @Test
    @DisplayName("Kayıt yoksa ya da pasifse 404; Keycloak'a gidilmez")
    void shouldRejectUnknownOwner() {
        LoginAccountRequest request = new LoginAccountRequest("x.y.z", "05321234567", null, List.of(SITE));
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.createSubcontractorLoginAccount(admin, unknown, request))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThat(opened).isEmpty();
        assertThat(codesSent).isEmpty();
    }

    @Test
    @DisplayName("Lokasyon boşsa ya da etkin değilse hesap açılmaz")
    void shouldRejectBadSites() {
        LoginAccountRequest none = new LoginAccountRequest("x.y.z", "05321234567", null, List.of());
        LoginAccountRequest inactive = new LoginAccountRequest("x.y.z", "05321234567", null, List.of(SITE, OTHER_SITE));

        assertThatThrownBy(() -> service.createSubcontractorLoginAccount(admin, SUBCONTRACTOR, none))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.createSubcontractorLoginAccount(admin, SUBCONTRACTOR, inactive))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(opened).isEmpty();
    }

    @Test
    @DisplayName("Hesap listesi giriş adı, ad ve e-postada büyük-küçük harf duyarsız aranır ve sayfalanır")
    void shouldSearchAndPageLoginAccounts() {
        existing.add(summary("ali.yilmaz", "Ali Yılmaz", "ali@ornek.com"));
        existing.add(summary("depo", "Depo Sorumlusu", "depo@fabrika.com"));
        existing.add(summary("fasoncu", "Örnek Tekstil", "info@ornek.com"));

        MasterDataPage<LoginAccountSummary> byEmail =
                service.listLoginAccounts(new MasterDataQuery("ORNEK.COM", 0, PAGE_SIZE));
        MasterDataPage<LoginAccountSummary> secondPage = service.listLoginAccounts(new MasterDataQuery(null, 1, 2));
        MasterDataPage<LoginAccountSummary> byName =
                service.listLoginAccounts(new MasterDataQuery("sorumlu", 0, PAGE_SIZE));

        assertThat(byEmail.items()).extracting(LoginAccountSummary::username).containsExactly("ali.yilmaz", "fasoncu");
        assertThat(byEmail.totalElements()).isEqualTo(2);
        assertThat(secondPage.items()).extracting(LoginAccountSummary::username).containsExactly("fasoncu");
        assertThat(secondPage.totalElements()).isEqualTo(ACCOUNT_COUNT);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(byName.items()).extracting(LoginAccountSummary::username).containsExactly("depo");
    }

    private static LoginAccountSummary summary(String username, String fullName, String email) {
        return new LoginAccountSummary(
                username, username, fullName, email, null, List.of("SUBCONTRACTOR"), true, false, null);
    }
}
