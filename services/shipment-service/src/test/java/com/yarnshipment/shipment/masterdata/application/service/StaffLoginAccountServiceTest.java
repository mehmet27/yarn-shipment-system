package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.out.LoginAccountTargetPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffAccountPort;
import com.yarnshipment.shipment.masterdata.application.port.out.StaffWorkAreaPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.StaffAccountSpec;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private final List<String> nameChanges = new ArrayList<>();
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

        @Override
        public void changeStaffName(UUID userId, String fullName) {
            nameChanges.add(userId + ":" + fullName);
        }
    };
    private final FakeCredentials credentials = new FakeCredentials();
    private final List<String> savedAreas = new ArrayList<>();
    private final StaffWorkAreaPort workAreas = new StaffWorkAreaPort() {
        @Override
        public void save(UUID userId, StaffWorkArea area) {
            savedAreas.add(userId + ":" + area);
        }

        @Override
        public void delete(UUID userId) {
            savedAreas.add(userId + ":deleted");
        }

        @Override
        public Map<UUID, StaffWorkArea> findAll() {
            return Map.of();
        }
    };
    private final StaffLoginAccountService service =
            new StaffLoginAccountService(targets, accounts, credentials, workAreas);

    @Test
    @DisplayName("Kalite hesabı çalışma alanıyla açılır ve alan yeni kullanıcıya yazılır")
    void shouldSaveWorkAreaForQuality() {
        service.createStaffLoginAccount(
                admin,
                new StaffAccountRequest(
                        "kalite1", "Ali Kalite", StaffRole.QUALITY, "Gecici-Sifre-1", StaffWorkArea.CENTRAL));

        assertThat(savedAreas).containsExactly(NEW_USER + ":CENTRAL");
    }

    @Test
    @DisplayName("Kalite hesabında alan zorunlu, diğer rollerde verilemez")
    void shouldRequireWorkAreaOnlyForQuality() {
        StaffAccountRequest noArea = new StaffAccountRequest("k", "Ali Kalite", StaffRole.QUALITY, "Gecici-Sifre-1");
        StaffAccountRequest extraArea =
                new StaffAccountRequest("d", "Ayşe Depo", StaffRole.WAREHOUSE, "Gecici-Sifre-1", StaffWorkArea.CENTRAL);

        assertThatThrownBy(() -> service.createStaffLoginAccount(admin, noArea))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.createStaffLoginAccount(admin, extraArea))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(openedStaff).isEmpty();
        assertThat(savedAreas).isEmpty();
    }

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
    @DisplayName("Ad soyad kırpılarak yazılır; kişi kendi adını da değiştirebilir, çok kısa ad reddedilir")
    void shouldChangeName() {
        UUID other = UUID.randomUUID();

        service.changeLoginAccountName(admin, other, "  Ayşe Yılmaz ");
        service.changeLoginAccountName(admin, admin.userId(), "Ali Veli");

        assertThat(nameChanges).containsExactly(other + ":Ayşe Yılmaz", admin.userId() + ":Ali Veli");
        assertThatThrownBy(() -> service.changeLoginAccountName(admin, other, " A "))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(nameChanges).hasSize(2);
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

        service.changeLoginAccountRole(admin, other, StaffRole.QUALITY, StaffWorkArea.SUBCONTRACTOR);

        assertThat(roleChanges).containsExactly(other + ":QUALITY");
        assertThat(savedAreas).containsExactly(other + ":SUBCONTRACTOR");
        assertThatThrownBy(() -> service.changeLoginAccountRole(admin, admin.userId(), StaffRole.MANAGER, null))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(roleChanges).hasSize(1);
    }

    @Test
    @DisplayName("Kalite dışı role geçince çalışma alanı silinir; kalite alansız ya da başka rol alanlı reddedilir")
    void shouldClearOrRequireWorkAreaOnRoleChange() {
        UUID other = UUID.randomUUID();

        service.changeLoginAccountRole(admin, other, StaffRole.MANAGER, null);

        assertThat(savedAreas).containsExactly(other + ":deleted");
        assertThatThrownBy(() -> service.changeLoginAccountRole(admin, other, StaffRole.QUALITY, null))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> service.changeLoginAccountRole(admin, other, StaffRole.MANAGER, StaffWorkArea.CENTRAL))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThat(roleChanges).hasSize(1);
    }
}
