package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.LoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccount;
import com.yarnshipment.shipment.masterdata.domain.model.LoginAccountSpec;
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

    private static final int PASSWORD_LENGTH = 16;
    private static final UUID DRIVER = UUID.fromString("0192f0a0-0000-7000-8000-000000000004");
    private static final UUID SUBCONTRACTOR = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final UUID OTHER_SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000002");

    private final List<LoginAccountSpec> opened = new ArrayList<>();
    private final Actor admin = TestTokens.actor("admin");
    private final LoginAccountTargetPort targets = new LoginAccountTargetPort() {
        @Override
        public Optional<String> findActiveDriverName(UUID id) {
            return DRIVER.equals(id) ? Optional.of("Ali Yılmaz") : Optional.empty();
        }

        @Override
        public Optional<String> findActiveSubcontractorTitle(UUID id) {
            return SUBCONTRACTOR.equals(id) ? Optional.of("Örnek Tekstil") : Optional.empty();
        }

        @Override
        public Set<UUID> findActiveSiteIds(Collection<UUID> ids) {
            return ids.contains(SITE) ? Set.of(SITE) : Set.of();
        }
    };
    private final LoginAccountPort accounts = opened::add;
    private final LoginAccountService service = new LoginAccountService(targets, accounts);

    @Test
    @DisplayName("Şoför için açılan hesap şoför rolü, kayıt kimliği ve üretilen geçici şifreyle istenir")
    void shouldOpenDriverAccount() {
        LoginAccount account = service.createDriverLoginAccount(
                admin, DRIVER, new LoginAccountRequest("ali.yilmaz", null, List.of(SITE)));

        assertThat(account.username()).isEqualTo("ali.yilmaz");
        assertThat(account.temporaryPassword()).hasSize(PASSWORD_LENGTH);
        assertThat(opened).singleElement().satisfies(spec -> {
            assertThat(spec.role()).isEqualTo(LoginRole.DRIVER);
            assertThat(spec.ownerId()).isEqualTo(DRIVER);
            assertThat(spec.displayName()).isEqualTo("Ali Yılmaz");
            assertThat(spec.temporaryPassword()).isEqualTo(account.temporaryPassword());
        });
        assertThat(account.toString()).doesNotContain(account.temporaryPassword());
    }

    @Test
    @DisplayName("Fasoncu hesabı fasoncu rolüyle istenir")
    void shouldOpenSubcontractorAccount() {
        service.createSubcontractorLoginAccount(
                admin, SUBCONTRACTOR, new LoginAccountRequest("ornek", "a@b.com", List.of(SITE)));

        assertThat(opened).singleElement().satisfies(spec -> {
            assertThat(spec.role()).isEqualTo(LoginRole.SUBCONTRACTOR);
            assertThat(spec.email()).isEqualTo("a@b.com");
        });
    }

    @Test
    @DisplayName("Kayıt yoksa ya da pasifse 404; Keycloak'a gidilmez")
    void shouldRejectUnknownOwner() {
        LoginAccountRequest request = new LoginAccountRequest("x.y.z", null, List.of(SITE));
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.createDriverLoginAccount(admin, unknown, request))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> service.createSubcontractorLoginAccount(admin, unknown, request))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThat(opened).isEmpty();
    }

    @Test
    @DisplayName("Lokasyon boşsa ya da etkin değilse hesap açılmaz")
    void shouldRejectBadSites() {
        LoginAccountRequest none = new LoginAccountRequest("x.y.z", null, List.of());
        LoginAccountRequest inactive = new LoginAccountRequest("x.y.z", null, List.of(SITE, OTHER_SITE));

        assertThatThrownBy(() -> service.createDriverLoginAccount(admin, DRIVER, none))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.createDriverLoginAccount(admin, DRIVER, inactive))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(opened).isEmpty();
    }
}
