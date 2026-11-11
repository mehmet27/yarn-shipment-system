package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaffLoginAccountServiceTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000001");
    private static final int EXPECTED_CHANGES = 3;
    private static final UUID NEW_USER = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");

    private final List<StaffAccountSpec> openedStaff = new ArrayList<>();
    private final List<String> roleChanges = new ArrayList<>();
    private final Actor admin = TestTokens.actor("admin");
    private Set<UUID> activeSites = Set.of(SITE);
    private final LoginAccountTargetPort targets = new LoginAccountTargetPort() {
        @Override
        public Set<UUID> findActiveSiteIds(Collection<UUID> ids) {
            return ids.stream().filter(activeSites::contains).collect(Collectors.toSet());
        }

        @Override
        public Set<UUID> findAllActiveSiteIds() {
            return activeSites;
        }
    };
    private final StaffAccountPort accounts = new StaffAccountPort() {
        @Override
        public UUID createStaff(StaffAccountSpec spec) {
            openedStaff.add(spec);
            return NEW_USER;
        }

        @Override
        public void changeStaffRole(UUID userId, StaffRole role) {
            roleChanges.add(userId + ":" + role);
        }
    };
    private final FakeCredentials credentials = new FakeCredentials();
    private final StaffLoginAccountService service = new StaffLoginAccountService(targets, accounts, credentials);

    @Test
    @DisplayName("Fabrika içi hesap yöneticinin şifresi, verilen rol ve etkin tüm lokasyonlarla açılır")
    void shouldOpenStaffAccountWithAdminPassword() {
        service.createStaffLoginAccount(
                admin, new StaffAccountRequest("depo2", "Ayşe Depo", StaffRole.WAREHOUSE, "Gecici-Sifre-1"));

        assertThat(openedStaff).singleElement().satisfies(spec -> {
            assertThat(spec.username()).isEqualTo("depo2");
            assertThat(spec.role()).isEqualTo(StaffRole.WAREHOUSE);
            assertThat(spec.password()).isEqualTo("Gecici-Sifre-1");
            assertThat(spec.siteIds()).containsExactly(SITE);
        });
    }

    @Test
    @DisplayName("Yönetici bir hesaba yeni geçici şifre belirler")
    void shouldSetTemporaryPassword() {
        UUID other = UUID.randomUUID();

        service.setLoginAccountPassword(admin, other, "Yeni-Sifre-2");

        assertThat(credentials.passwords()).containsExactly(other + ":Yeni-Sifre-2");
    }

    @Test
    @DisplayName("Hesap başkası için kapatılıp açılabilir; kişi kendi hesabını kapatamaz")
    void shouldDisableOthersOnly() {
        UUID other = UUID.randomUUID();

        service.disableLoginAccount(admin, other);
        service.enableLoginAccount(admin, other);
        service.enableLoginAccount(admin, admin.userId());

        assertThat(credentials.enabledChanges())
                .containsExactly(other + ":false", other + ":true", admin.userId() + ":true");
        assertThatThrownBy(() -> service.disableLoginAccount(admin, admin.userId()))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(credentials.enabledChanges()).hasSize(EXPECTED_CHANGES);
    }

    @Test
    @DisplayName("Etkin lokasyon yoksa hesap açılmaz")
    void shouldRejectWhenNoActiveSite() {
        activeSites = Set.of();
        StaffAccountRequest request = new StaffAccountRequest("d", "Ayşe Depo", StaffRole.QUALITY, "Gecici-Sifre-1");

        assertThatThrownBy(() -> service.createStaffLoginAccount(admin, request))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(openedStaff).isEmpty();
    }

    @Test
    @DisplayName("Rol başkasının hesabında değişir; kişi kendi rolünü değiştiremez")
    void shouldChangeRoleOfOthersOnly() {
        UUID other = UUID.randomUUID();

        service.changeLoginAccountRole(admin, other, StaffRole.QUALITY);

        assertThat(roleChanges).containsExactly(other + ":QUALITY");
        assertThatThrownBy(() -> service.changeLoginAccountRole(admin, admin.userId(), StaffRole.MANAGER))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(roleChanges).hasSize(1);
    }
}
